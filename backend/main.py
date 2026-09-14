from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from database import SessionLocal, engine
import models, schemas

# Garante que as tabelas existem no banco
models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="Radar de Alagamentos API")

# Dependência do BD
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.get("/health")
def health_check():
    return {"status": "ok", "message": "O servidor está rodando e conectado ao BD!"}

import pika
import json

@app.post("/alertas")
def criar_alerta(alerta: schemas.AlertaCreate):
    # Publicar na fila do RabbitMQ
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost', port=5672))
    channel = connection.channel()
    channel.queue_declare(queue='alertas_queue')

    mensagem = {
        "tipo_alerta": alerta.tipo_alerta,
        "latitude": alerta.latitude,
        "longitude": alerta.longitude,
        "observacao": alerta.observacao,
        "usuario": alerta.usuario
    }
    
    channel.basic_publish(exchange='', routing_key='alertas_queue', body=json.dumps(mensagem))
    connection.close()
    
    # Retorna o alerta fake para o Android não quebrar o parser do Retrofit
    return {
        "id": 9999,
        "tipo_alerta": alerta.tipo_alerta,
        "latitude": alerta.latitude,
        "longitude": alerta.longitude,
        "observacao": alerta.observacao,
        "usuario": alerta.usuario,
        "status": "processando"
    }

from typing import List, Optional
from sqlalchemy import func

@app.get("/alertas", response_model=List[schemas.AlertaResponse])
def listar_alertas(
    lat: Optional[float] = None, 
    lng: Optional[float] = None, 
    raio_km: float = 5.0, 
    db: Session = Depends(get_db)
):
    # Retorna tanto os ativos quanto os removidos para que o histórico saiba do soft-delete
    query = db.query(models.Alerta).filter(models.Alerta.status.in_(["ativo", "removido"]))
    
    if lat is not None and lng is not None:
        ponto_usuario = f"SRID=4326;POINT({lng} {lat})"
        query = query.filter(
            func.ST_DWithin(
                func.cast(models.Alerta.localizacao, func.Geography()), 
                func.cast(func.ST_GeomFromEWKT(ponto_usuario), func.Geography()), 
                raio_km * 1000
            )
        )
        
    return query.all()

@app.put("/alertas/{alerta_id}/remover")
def remover_alerta(alerta_id: int, db: Session = Depends(get_db)):
    alerta = db.query(models.Alerta).filter(models.Alerta.id == alerta_id).first()
    if not alerta:
        raise HTTPException(status_code=404, detail="Alerta não encontrado")
    
    if alerta.status == "removido":
        raise HTTPException(status_code=400, detail="Alerta já foi removido")
        
    alerta.status = "removido"
    db.commit()
    return {"status": "removido com sucesso", "id": alerta.id}
