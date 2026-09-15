import pika
import json
from database import SessionLocal
import models
import firebase_admin
from firebase_admin import credentials, messaging

# Inicializa o Firebase
cred = credentials.Certificate("firebase-adminsdk.json")
firebase_admin.initialize_app(cred)

def enviar_push_notification(alerta):
    # Envia apenas 'data' para que o Android intercepte silenciosamente
    # e decida se deve mostrar o Pop-up com base na distancia (< 3km) e autor
    mensagem_push = messaging.Message(
        data={
            "tipo_alerta": alerta.tipo_alerta,
            "usuario": alerta.usuario,
            "latitude": str(alerta.latitude),
            "longitude": str(alerta.longitude),
            "id": str(alerta.id) if alerta.id else ""
        },
        topic="alertas"
    )
    resposta = messaging.send(mensagem_push)
    print(f" [OK] Push Notification enviada com sucesso: {resposta}")

def callback(ch, method, properties, body):
    # Converte o JSON recebido de volta para um dicionário
    mensagem = json.loads(body)
    print(f" [x] Recebido da Fila RabbitMQ: {mensagem}")

    # Processa e salva no banco de dados
    db = SessionLocal()
    try:
        ponto_wkt = f"POINT({mensagem['longitude']} {mensagem['latitude']})"
        
        novo_alerta = models.Alerta(
            tipo_alerta=mensagem['tipo_alerta'],
            latitude=mensagem['latitude'],
            longitude=mensagem['longitude'],
            observacao=mensagem.get('observacao'),
            localizacao=ponto_wkt,
            status="ativo",
            usuario=mensagem.get('usuario', 'Anônimo')
        )
        db.add(novo_alerta)
        db.commit()
        print(" [x] Salvo com sucesso no PostgreSQL (PostGIS)!")
        
        # Dispara o Push Notification
        enviar_push_notification(novo_alerta)
        
    except Exception as e:
        print(f" [!] Erro ao salvar/notificar: {e}")
        db.rollback()
    finally:
        db.close()

def iniciar_worker():
    print(' [*] Conectando ao RabbitMQ...')
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost', port=5672))
    channel = connection.channel()

    channel.queue_declare(queue='alertas_queue')

    channel.basic_consume(queue='alertas_queue', on_message_callback=callback, auto_ack=True)

    print(' [*] Aguardando mensagens na fila. Pressione CTRL+C para sair')
    channel.start_consuming()

if __name__ == '__main__':
    iniciar_worker()

