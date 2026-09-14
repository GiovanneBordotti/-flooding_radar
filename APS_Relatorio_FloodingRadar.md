# UNIVERSIDADE PAULISTA – UNIP
## INSTITUTO DE CIÊNCIAS EXATAS E TECNOLOGIA
## CURSO DE CIÊNCIA DA COMPUTAÇÃO

**GIOVANNE BORDOTTI - RA: [Preencher]**

# PROJETO: FLOODING RADAR
## SISTEMA DISTRIBUÍDO DE ALERTA DE ALAGAMENTOS E QUEDAS DE ÁRVORES COM ARQUITETURA OFFLINE-FIRST

**SÃO PAULO**
**2026**

---

# 1. OBJETIVO DO TRABALHO
O projeto "Flooding Radar" é um sistema distribuído projetado para permitir que os cidadãos reportem e consultem incidentes urbanos críticos, como alagamentos e quedas de árvores, em tempo real. O objetivo principal da aplicação é prover uma plataforma georreferenciada de alta resiliência (Offline-First), capaz de salvar as vidas dos moradores e proteger seus bens materiais. Utilizando uma arquitetura moderna baseada em Microsserviços, Mensageria e Bancos Geoespaciais, a plataforma permite a sobrevivência da aplicação mesmo em cenários de queda de conectividade, comuns durante grandes tempestades. 

Adicionalmente, a aplicação introduziu a funcionalidade de "Radar Dinâmico" e notificações Push geolocalizadas, criando um ecossistema inteligente em que os cidadãos são avisados proativamente dos perigos apenas quando estes representam um risco real (distância configurável de 1km a 10km).

---
# 2. JUSTIFICATIVA E MOTIVAÇÃO DA ESCOLHA DO PROJETO E LINGUAGEM
Em grandes metrópoles, os eventos climáticos extremos têm se tornado cada vez mais frequentes. Aplicativos convencionais perdem sua utilidade justamente no momento em que são mais necessários: quando a infraestrutura de rede (4G/Wi-Fi) cai devido às tempestades. 

A construção de uma arquitetura Distribuída com **Offline-First** resolve esse dilema. O usuário pode registrar ocorrências offline e o sistema ("SyncWorker") armazena localmente, sincronizando com a nuvem quando a conexão retornar. 

As tecnologias escolhidas refletem o que há de mais moderno na indústria de sistemas distribuídos:
* **Kotlin (Jetpack Compose, Room, WorkManager, Broadcasts):** Linguagem moderna para Android, permitindo criar rotinas de sincronização tolerantes a falhas e interfaces reativas (Radar Visual).
* **Python (FastAPI, Pika/RabbitMQ):** Escolhido por sua agilidade e ecossistema assíncrono. O backend atua como um Gateway rápido que delega tarefas pesadas.
* **PostgreSQL + PostGIS:** Indispensável para armazenar dados físicos (Lat/Lng) e realizar cálculos algébricos e geométricos complexos na camada de banco de dados (Ex: Raio de distância).
* **Firebase Cloud Messaging (FCM) via Data Payload:** Essencial para entrega silenciosa de eventos em tempo real, permitindo aos clientes recalcular distâncias internamente e atualizar a tela simultaneamente.
* **Ngrok:** Utilizado na fase de homologação para expor o servidor local à internet (WAN), permitindo o teste de dois celulares físicos utilizando redes distintas (Wi-Fi e 4G) e validando o tráfego global.

---
# 3. CONCEITOS E FUNÇÕES DA LINGUAGEM DE PROGRAMAÇÃO

**No Backend (Python):**
* **Injeção de Dependência:** O FastAPI utiliza injeção de dependência nativa (ex: obter a sessão do banco local).
* **Assincronismo:** Uso ostensivo de mensageria assíncrona (RabbitMQ) para desacoplar a recepção da requisição de inserção do processamento pesado. O produtor `main.py` entrega à fila e o consumidor `worker.py` efetiva a lógica.
* **Processamento de Push Georreferenciado:** Uso da SDK do Firebase (`firebase-admin`) enviando pacotes customizados (Data Payload) via dicionários estruturados, permitindo que a regra de negócio fique no cliente.

**No Frontend (Kotlin/Android):**
* **Coroutines e Flows:** Utilizados exaustivamente no `MapViewModel` para não bloquear a Thread Principal, assegurando 60fps no mapa. A reatividade do `StateFlow` garante a renderização Otimista da Interface.
* **WorkManager (SyncWorker):** Serviço persistente do sistema operacional Android que aplica "Exponential Backoff" para garantir a sincronia dos alertas que falharam inicialmente.
* **LocalBroadcastManager:** Comunicação Inter-Processos (IPC) leve e segura para sinalizar atualizações em tempo real entre o `MyFirebaseMessagingService` e a `MapScreen` nativa, sem reabrir a Activity.
* **SharedPreferences:** Armazenamento chave-valor (chave = nome, raio_km) que dita o comportamento dinâmico do Radar.

---
# 4. TRABALHOS RELACIONADOS
O Waze foi a principal fonte de inspiração conceitual. Ele popularizou o "Crowdsourcing" para relatar incidentes. No entanto, sua natureza é focada na condução e depende severamente da conexão contínua. 
Sistemas governamentais (ex: Sirenes da Defesa Civil) são unidirecionais (apenas avisam). O Flooding Radar se destaca por unir o "Crowdsourcing" do Waze com a Resiliência de dados governamentais e filtros por zona de impacto hiperlocal.

---
# 5. CARACTERÍSTICAS DA APLICAÇÃO COMO UM SISTEMA DISTRIBUÍDO

1. **Desacoplamento e Mensageria (RabbitMQ):** A API `main.py` não salva os dados no banco diretamente. Ela publica o alerta no RabbitMQ. Um `worker.py` autônomo assina essa fila, realiza o *insert* na tabela espacial e chama a API do Firebase. Isso garante que picos súbitos de acesso durante enchentes não derrubem o Banco de Dados (Load Leveling).
2. **Consistência Eventual:** O sistema admite o Teorema CAP. Para manter alta Disponibilidade (A) e Tolerância a Partição (P), a Consistência (C) é eventual. O usuário vê seu próprio alerta (UI Otimista) e os demais a receberão segundos depois, via notificação.
3. **Filtro de Tráfego e Redução de Banda:** A filtragem de notificações ocorre silenciosamente na "Borda" (Edge Computing). O Payload da notificação (com Lat/Lng) é recebido pelo celular, que realiza a Fórmula de Haversine localmente. Se o evento ocorreu além do limite do usuário (ex: 5km), a notificação é suprimida. Isso desafoga o backend e melhora a UX.
4. **Soft Delete Distribuído:** Quando um alerta é removido via `PUT /alertas/{id}/remover`, ele apenas muda de status (`removido`) no PostGIS, sem deletar fisicamente. Assim, celulares consultando a malha garantem auditoria do que foi cancelado.

---
# 6. PLANO DE DESENVOLVIMENTO DA APLICAÇÃO

O desenvolvimento seguiu métodos ágeis (fases evolutivas):

**Fase 1: Infraestrutura (Semana 1)**
* Configuração do Kotlin e Google Maps SDK.
* Contêineres Docker com PostgreSQL + PostGIS.

**Fase 2: Comunicação Síncrona e APIs (Semana 2)**
* API FastAPI com rotas HTTP REST (`GET /alertas`, `PUT /alertas/{id}/remover`).
* Conexão WAN testada utilizando Ngrok (testes simulando redes 4G vs Wi-Fi).

**Fase 3: Offline-First e Consistência (Semana 3)**
* Criação de tabelas nativas com `Room` no Android.
* Implementação do `SyncWorker` engatilhado por redes ativas.

**Fase 4: Mensageria Assíncrona e Push Silencioso (Semana 4)**
* Arquitetura em Worker (`worker.py`) consumindo RabbitMQ.
* Uso de FCM Data Payload. Ao interceptar, o celular calcula a distância.
* Implementação do `LocalBroadcast` para Real-Time Refresh da UI e supressão de Auto-Notificação (autor não recebe).

**Fase 5: UX Avançada, Radar e Soft Delete (Semana Final)**
* Círculo Azul estático e transparente demarcando a área do Radar visualmente (baseado na preferência).
* Interface de remoção de alertas (Lixeira no InfoWindow do Marker).
* Redirecionamento da Notificação direto para a localização (Latitude e Longitude do evento via PendingIntent).

---
# 7. PROJETO (ESTRUTURA E MÓDULOS) DO PROGRAMA

O sistema foi arquitetado nos seguintes módulos distribuídos:

**7.1. Módulo Mobile (Nó Cliente - Edge Android)**
* **SettingsScreen:** Coleta preferências em `SharedPreferences` (Raio customizável de 1km a 10km, nome do usuário, status da notificação).
* **MapScreen:** Renderiza os marcadores e a geometria do `Circle` do radar dinâmico. Pede Permissões Seguras (API 33+) via Contratos e atualiza passivamente ouvindo Broadcasts. Implementa modal interativa para reportes rápidos de "Alagamento", "Queda de Árvore" ou "Outro".
* **MyFirebaseMessagingService:** Interceptador que avalia distâncias usando a última localização e envia a notificação se pertinente. Emite um Broadcast invisível instruindo o Retrofit a regerar os dados (Tempo real).
* **SyncWorker:** Garante que reportes sem internet atinjam a nuvem posteriormente.

**7.2. Módulo API de Ingestão (FastAPI Gateway)**
* Roteador rápido que, em métodos de escrita (POST), apenas converte JSON em String e serializa na Fila do RabbitMQ, retornando HTTP 202/200 Otimista. Em métodos de leitura (GET/PUT), acessa diretamente o banco para latência zero no mapa.

**7.3. Módulo Processamento (Workers e PostGIS)**
* **RabbitMQ Broker:** Barramento que isola e orquestra mensagens.
* **Worker (Consumidor):** Realiza transações ACID no PostgreSQL convertendo pontos `Latitude/Longitude` em `SRID 4326 POINT()`. Contata a API do Google Cloud para disparar a notificação.

---
# 8. RELATÓRIO COM AS LINHAS DE CÓDIGO DO PROGRAMA

**8.1. Android: Filtro Silencioso de Raio de Notificação (Edge Computing)**
```kotlin
// MyFirebaseMessagingService.kt - Processamento do Data Payload na borda
val distanceMeters = userLocation.distanceTo(alertLocation)
val limiteMeters = sharedPref.getInt("notificacoes_raio_km", 3) * 1000.0

if (distanceMeters > limiteMeters) {
    return // Ignora a notificação pois está além do raio configurado
}
mostrarNotificacao(titulo, mensagem, latStr, lngStr)
```

**8.2. Android: Sincronização Automática via Broadcast (Real-Time)**
```kotlin
// MainActivity.kt - Mapa se atualizando sozinho
private val refreshReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        mapViewModel.buscarAlertas()
    }
}
LocalBroadcastManager.getInstance(this).registerReceiver(refreshReceiver, IntentFilter("UPDATE_ALERTS"))
```

**8.3. Python: Desacoplamento via RabbitMQ (Gateway vs Worker)**
```python
# main.py
def criar_alerta(alerta: schemas.AlertaCreate):
    channel.basic_publish(exchange='', routing_key='alertas_queue', body=json.dumps(alerta.dict()))
    return {"status": "processando"}

# worker.py
def callback(ch, method, properties, body):
    mensagem = json.loads(body)
    novo_alerta = models.Alerta(localizacao=f"POINT({mensagem['longitude']} {mensagem['latitude']})", ...)
    db.add(novo_alerta)
    db.commit()
    enviar_push_notification(novo_alerta)
```

**8.4. Android: Radar Dinâmico Visual (Google Maps Compose)**
```kotlin
// MapScreen.kt - Círculo estático transparente de alto desempenho
val raioKm = sharedPref.getInt("notificacoes_raio_km", 3)
userLocation?.let { loc ->
    Circle(
        center = loc,
        radius = raioKm * 1000.0,
        fillColor = androidx.compose.ui.graphics.Color(0x0A0000FF), 
        strokeColor = androidx.compose.ui.graphics.Color(0x330000FF), 
        strokeWidth = 2f
    )
}
```

---
# 9. APRESENTAÇÃO DO PROGRAMA EM FUNCIONAMENTO

A validação do projeto em um cenário real abrangeu:
1. **Redes Distintas:** Hospedagem da aplicação em servidor local (Windows) exposta publicamente via tunelamento `Ngrok`.
2. **Dispositivos Físicos:** O APK `FloodingRadar_Final.apk` instalado em dois smartphones físicos distintos. O Celular 1 no 4G (Rua) e o Celular 2 no Wi-Fi (Casa).
3. **Casos de Uso Aprovados:**
   - Celular 1 postou um alerta. 
   - A fila RabbitMQ engoliu o post, o worker processou e emitiu Push.
   - O Celular 2 (estando dentro do raio configurado de 5km de distância) recebeu o Data Payload silencioso.
   - O mapa do Celular 2 se auto-atualizou magicamente sem intervenção.
   - O Celular 1 (autor) filtrou sua própria notificação para não ser bombardeado.
   - Um toque na notificação do Celular 2 e a câmera do mapa viajou (Fly-to) animadamente até a rua exata (Zoom 17f) provida por Alta Precisão (GPS Provider).
4. **Resiliência:** Ao desativar os dados móveis, posts novos eram armadilhados no banco local (`Room`). Quando a rede retornava, o `SyncWorker` despertava e limpava o backlog (Consistência Eventual).

---
# 10. BIBLIOGRAFIA

1. COULOURIS, George; DOLLIMORE, Jean; KINDBERG, Tim; BLAIR, Gordon. **Sistemas Distribuídos: Conceitos e Projeto.** 5. ed. Porto Alegre: Bookman, 2013.
2. GOOGLE. **Guia para Arquitetura do App (Android Jetpack e WorkManager).** Disponível em: https://developer.android.com/jetpack/guide. Acesso em: 13 set. 2026.
3. RABBITMQ. **RabbitMQ Tutorials (Python).** Disponível em: https://www.rabbitmq.com/getstarted.html. Acesso em: 13 set. 2026.
4. FIREBASE. **Firebase Cloud Messaging Data Payloads.** Disponível em: https://firebase.google.com/docs/cloud-messaging. Acesso em: 13 set. 2026.

---
# 11. FICHA DE ATIVIDADES PRÁTICAS SUPERVISIONADAS

*Preencher no documento oficial da universidade, ilustrando as datas correspondentes:*
* Definição da Arquitetura C/S e Contêineres Docker (PostgreSQL+PostGIS).
* Implementação das rotas REST (FastAPI) e modelagem de mapas nativos.
* Implementação do Room, WorkManager e Consultas Geoespaciais offline.
* Refatoração Distribuída: Mensageria RabbitMQ e FCM Integrado.
* Homologação: Configuração de Tunelamento Ngrok, Broadcasts Real-Time e UI de Radar Dinâmico. Redação do relatório.
