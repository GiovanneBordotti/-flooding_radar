# ATIVIDADES PRÁTICAS SUPERVISIONADAS (APS)
## Curso: [Nome do Curso - ex: Ciência da Computação / Engenharia de Software]
## Tema: Radar de Alagamentos - Sistema Distribuído Mobile com Offline-First
## Grupo: 
- Nome do Aluno 1 - RA: XXXXXX
- Nome do Aluno 2 - RA: XXXXXX
- Nome do Aluno 3 - RA: XXXXXX
- Nome do Aluno 4 - RA: XXXXXX

---
# 2. ÍNDICE
1. Objetivo e Motivação do Trabalho
2. Introdução
3. Fundamentos das Tecnologias para Dispositivos Móveis Escolhidas
4. Plano de Desenvolvimento da Aplicação
5. Projeto (Estrutura e Módulos) do Programa
6. Relatório com as Linhas de Código do Programa
7. Apresentação do Programa em Funcionamento
8. Bibliografia
9. Ficha de Atividades Práticas Supervisionadas (Anexo)

---
# 3. OBJETIVO E MOTIVAÇÃO DO TRABALHO

**Objetivo:**
O objetivo principal deste trabalho é projetar, desenvolver e validar um Sistema Distribuído com arquitetura cliente-servidor (Edge-to-Cloud) focado no mapeamento colaborativo de incidentes climáticos urbanos, como alagamentos e quedas de árvores. A aplicação móvel serve como um nó inteligente na borda (Edge Node), capaz de operar em modo offline com consistência eventual.

**Motivação:**
Com as crescentes mudanças climáticas, centros urbanos sofrem constantemente com chuvas fortes que paralisam a mobilidade (trânsito, transporte público, vias alagadas). Aplicativos de trânsito tradicionais não têm foco exclusivo no clima. Desenvolver uma solução nativa, distribuída e resiliente, que funcione mesmo quando a rede 3G/4G cai no meio da tempestade (Offline-first), não é apenas um desafio acadêmico excelente para a disciplina de Sistemas Distribuídos, mas também uma ferramenta de grande impacto social.

---
# 4. INTRODUÇÃO

Sistemas Distribuídos modernos deixaram de ser apenas múltiplos servidores em um data center para abraçar bilhões de dispositivos móveis. Neste cenário, o smartphone não é apenas uma interface de usuário (View), mas um nó autônomo capaz de processar dados locais, gerenciar falhas de rede e sincronizar estados com um servidor central.

Este projeto, intitulado "Flooding Radar", aborda a complexidade da computação móvel em ambientes hostis de conectividade. Quando ocorrem tempestades, a infraestrutura de telecomunicações frequentemente falha. Como um sistema de alertas pode ser confiável se depende da internet no momento exato da crise? 

A resposta proposta neste trabalho é a implementação do padrão **Offline-First**. Utilizando banco de dados local embarcado e filas de trabalho (WorkManager), garantimos que nenhum dado de telemetria ou reporte de usuário seja perdido. Em paralelo, o back-end adota arquitetura orientada a mensagens (RabbitMQ) e bancos de dados geoespaciais (PostGIS), processando a informação de forma assíncrona e realizando o broadcast (Push Notifications) para alertar os demais nós do sistema em tempo real.

---
# 5. FUNDAMENTOS DAS TECNOLOGIAS PARA DISPOSITIVOS MÓVEIS ESCOLHIDAS

Nesta seção, fundamentamos a escolha da stack tecnológica que viabiliza a arquitetura distribuída:

**5.1. Android Nativo (Kotlin) e Jetpack Compose**
O desenvolvimento nativo permite acesso direto aos serviços de localização (Fuses Location Provider) e otimização de bateria. O Jetpack Compose foi escolhido por representar o estado da arte em interfaces declarativas, permitindo reatividade aos fluxos de dados assíncronos.

**5.2. Padrão Arquitetural MVVM (Model-View-ViewModel)**
O MVVM foi adotado para separar a interface gráfica da lógica de negócios. No contexto de sistemas distribuídos, o ViewModel atua como um coordenador que decide se os dados devem ser lidos do cache local ou buscados da rede.

**5.3. Room Database (SQLite)**
O Room atua como a camada de persistência local (o nó de armazenamento da borda). Ele é fundamental para a resiliência do sistema, armazenando relatórios com status de "pendente" quando a API não está acessível.

**5.4. WorkManager (Consistência Eventual)**
O WorkManager gerencia tarefas garantidas em background. Ele monitora os eventos de conectividade do Sistema Operacional e, assim que a rede é restabelecida, realiza a sincronização (Eventual Consistency) sem intervenção do usuário.

**5.5. Retrofit (Comunicação REST/JSON)**
Biblioteca padrão para requisições HTTP em Android, utilizada para a troca de mensagens JSON (Serialização/Desserialização) entre o nó cliente e o servidor.

**5.6. Firebase Cloud Messaging (FCM)**
O FCM atua como barramento de mensageria para push notifications. Em vez dos clientes realizarem "Polling" (sobrecarregando a rede), o Firebase permite o "Push", notificando passivamente os nós sobre enchentes no raio geográfico próximo.

---
# 6. PLANO DE DESENVOLVIMENTO DA APLICAÇÃO

O desenvolvimento foi faseado para garantir a construção iterativa e validação de componentes isolados do sistema distribuído.

**Fase 1: Infraestrutura (Semana 1)**
* Configuração do Android Studio e Kotlin.
* Implementação do mapa base (Google Maps SDK).
* Criação de containers Docker para o banco de dados PostgreSQL com a extensão PostGIS habilitada.

**Fase 2: Comunicação Síncrona (Caminho Feliz) (Semana 2)**
* Criação da API RESTful usando FastAPI (Python).
* Endpoint GET `/alertas` (Casting JSON).
* Integração do Retrofit no Android para comunicação ponta-a-ponta (Client-Server Request/Reply).

**Fase 3: Resiliência e Offline-First (Semana 3)**
* Modelagem do banco local com `AlertaEntity` e `AlertaDao` (Room).
* Desenvolvimento do `SyncWorker` (WorkManager) para tratar "Network Partition" (Teorema CAP - Foco em Disponibilidade e Tolerância a Partição).
* Consultas geoespaciais refinadas no PostGIS (`ST_DWithin`) para devolver apenas dados em um raio de 5km, economizando banda.

**Fase 4: Mensageria Assíncrona e Fator Visual (Semana 4)**
* Substituição de inserções diretas no banco por filas de mensagens (RabbitMQ) na API FastAPI.
* Criação de um Trabalhador secundário (`worker.py`) para consumir a fila.
* Integração do Firebase (FCM) no Python e Android para disparar alertas visuais ("Árvore caída a 2km").

**Fase 5: UX Avançada e Identidade (Opcional/Semana Final)**
* Implementação do Menu Inicial (`HomeScreen`) para captação do nome do usuário.
* Persistência de preferências de login usando `SharedPreferences` (Evitando re-autenticação contínua).
* Aba de listagem com cálculo de Distância Euclidiana e Geocodificação Reversa (Geocoder) assíncrona para extração de logradouros.

---
# 7. PROJETO (ESTRUTURA E MÓDULOS) DO PROGRAMA

O sistema foi arquitetado em três grandes macro-módulos distribuídos:

**7.1. Módulo Mobile (Nó Cliente - Edge)**
* **View (MapScreen / HomeScreen):** Gerencia a navegação inicial, renderiza o mapa, Menu Drawer para gerenciar estado offline e listar os alertas próximos (3km).
* **ViewModel (MapViewModel):** Coordena requisições. Tenta gravar no Room e enfileira o Worker. Coleta StateFlows do banco local. Emprega cálculo de distâncias (Location.distanceBetween).
* **Worker (SyncWorker):** Serviço em segundo plano. Acordado pelo Android quando há sinal 3G/Wi-Fi. Lê a tabela SQL local e envia via Retrofit.

**7.2. Módulo API de Ingestão (FastAPI Gateway)**
* **main.py:** Porta de entrada REST. Quando recebe um POST, ele não acessa o banco de dados. Ele age como um produtor (Producer), serializando o pacote e publicando no barramento do RabbitMQ (Fila `alertas_queue`). Retorna status 200/Enfileirado rapidamente para o Mobile. O schema de dados garante rastreabilidade do autor (campo `usuario`).

**7.3. Módulo Processamento e Notificação (Workers e Banco)**
* **RabbitMQ Broker:** Retém as mensagens em memória na porta 5672 até que haja trabalhadores disponíveis, nivelando picos de tráfego (Buffer).
* **worker.py (Consumer):** Executa o processamento pesado. Lê a fila, realiza operações espaciais no PostGIS (inserindo geometrias tipo POINT) e consome a SDK do Firebase Admin para enviar Push Notifications via Tópico ("alertas"), informando também quem realizou o registro.
* **PostgreSQL + PostGIS:** Armazena os dados físicos com suporte a álgebra geoespacial.

---
# 8. RELATÓRIO COM AS LINHAS DE CÓDIGO DO PROGRAMA

*(Nota: Nesta seção, cole trechos chaves que provam a aplicação de Sistemas Distribuídos)*

**8.1. Android: Geocodificação Assíncrona e Rastreio Espacial (Coroutines + Geocoder)**
```kotlin
// Thread Secundária para não travar a interface ao bater em APIs de Geocoding
LaunchedEffect(p) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val geocoder = android.location.Geocoder(context, java.util.Locale("pt", "BR"))
        val addresses = geocoder.getFromLocation(p.latitude, p.longitude, 1)
        rua = addresses?.firstOrNull()?.thoroughfare ?: "Rua desconhecida"
    }
}
```

**8.2. Android: Sincronização Tolerante a Falha (WorkManager)**
```kotlin
// SyncWorker.kt - Executado em background quando a internet volta
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).alertaDao()
        val pendentes = dao.getAlertasPendentes()
        try {
            pendentes.forEach { alerta ->
                RetrofitClient.api.criarAlerta(Alerta(
                    tipo_alerta = alerta.tipo_alerta, latitude = alerta.latitude, longitude = alerta.longitude
                ))
                dao.updateStatus(alerta.id, "enviado")
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry() // O Android tentará novamente depois (Backoff)
        }
    }
}
```

**8.2. Python: Fila de Mensagens (RabbitMQ)**
```python
# main.py - API Recebe e joga para a Fila (Desacoplamento)
@app.post("/alertas")
def criar_alerta(alerta: schemas.AlertaCreate):
    connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
    channel = connection.channel()
    channel.queue_declare(queue='alertas_queue')
    channel.basic_publish(exchange='', routing_key='alertas_queue', body=json.dumps(mensagem))
    connection.close()
    return {"status": "processando"}
```

**8.3. Python: Banco Geoespacial PostGIS**
```python
# Consulta Espacial limitando a busca em um raio de 5Km
ponto = f"SRID=4326;POINT({lng} {lat})"
query.filter(
    func.ST_DWithin(
        func.cast(models.Alerta.localizacao, func.Geography()), 
        func.cast(func.ST_GeomFromEWKT(ponto), func.Geography()), 
        5000 # metros
    )
)
```

---
# 9. APRESENTAÇÃO DO PROGRAMA EM FUNCIONAMENTO

A validação prática foi feita através da simulação de Network Partition (corte de rede) via Android Emulator. 
1. O usuário desativa o Wi-Fi. 
2. Realiza o reporte de um "Alagamento". O aviso flutuante "Será enviado assim que houver rede" é exibido. 
3. No Menu "Alertas Offline", o incidente consta pendente.
4. O Wi-Fi é reativado. O sistema operacional desperta o `SyncWorker`.
5. Os logs do RabbitMQ (`worker.py`) confirmam o recebimento assíncrono.
6. Uma notificação Push (FCM) é disparada no topo da tela validando a entrega de ponta-a-ponta (End-to-End).

---
# 10. BIBLIOGRAFIA

1. COULOURIS, George; DOLLIMORE, Jean; KINDBERG, Tim; BLAIR, Gordon. **Sistemas Distribuídos: Conceitos e Projeto.** 5. ed. Porto Alegre: Bookman, 2013.
2. GOOGLE. **Guia para Arquitetura do App (Android Jetpack).** Disponível em: https://developer.android.com/jetpack/guide. Acesso em: 13 set. 2026.
3. RAMALHO, Luciano. **Python Fluente.** Novatec Editora, 2015.
4. POSTGIS. **PostGIS Spatial and Geographic Objects for PostgreSQL.** Disponível em: https://postgis.net/. Acesso em: 13 set. 2026.
5. FIREBASE. **Firebase Cloud Messaging Documentation.** Disponível em: https://firebase.google.com/docs/cloud-messaging. Acesso em: 13 set. 2026.

---
# 11. FICHA DE ATIVIDADES PRÁTICAS SUPERVISIONADAS

*Preencher no documento oficial da universidade, ilustrando:*
* Data X: Definição da Arquitetura e Configuração Docker (Fase 1).
* Data Y: Implementação das rotas REST e Testes no Postman (Fase 2).
* Data Z: Implementação do Room, WorkManager e Consultas ST_DWithin (Fase 3).
* Data W: Refatoração para Mensageria RabbitMQ e Integração Firebase (Fase 4).
* Data V: Redação do relatório e validação final.

