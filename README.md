# DYSONOS V2

**Autonomous Network Infrastructure**

DYSONOS V2 é um simulador inteligente de infraestrutura de redes distribuídas. O backend permite cadastrar Networks, Zones e Nodes e associá-los em uma hierarquia.

## Tecnologias

### Backend implementado

- Java 21 e Spring Boot 3.5
- Spring Web, Spring Data JPA e Spring Validation
- PostgreSQL
- Maven
- JUnit 5 e Mockito
- Testcontainers para testes de integração com PostgreSQL

### Frontend implementado

- React 19 e TypeScript
- Vite e Tailwind CSS
- Three.js com React Three Fiber para topologia 3D
- Recharts para séries de métricas coletadas durante a sessão

O painel consome apenas rotas que já existem no backend. Histórico de eventos fica na sessão do navegador; segurança e expansão automática estão identificadas como pendentes de integração.

## Status

Backend de Networks, Zones, Nodes, métricas e Workloads implementado e validado. O frontend inicial está em desenvolvimento.

## Requisitos

- JDK 21
- Maven 3.6.3 ou superior
- Docker Desktop com Docker Compose (também usado pelos testes de integração) ou PostgreSQL instalado localmente
- Node.js 20 ou superior e npm

## Executar

Na raiz do projeto, defina uma senha local fora dos arquivos versionados e inicie o PostgreSQL. No PowerShell:

```powershell
$env:DB_PASSWORD = "sua-senha-local"
docker compose up -d postgres
```

Em outro terminal, execute a aplicação:

```powershell
cd backend
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8080`. Para executar os testes:

```powershell
cd backend
mvn test
```

Os testes de integração criam um PostgreSQL temporário pelo Testcontainers. Deixe o Docker Desktop iniciado para executá-los; sem acesso ao Docker, eles são ignorados e os demais testes continuam rodando.

As configurações do banco são definidas pelas variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. A senha é fornecida pelo ambiente e não é armazenada no repositório.

## Executar o frontend

Com o backend e o PostgreSQL rodando em outro terminal:

```powershell
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. O Vite encaminha as chamadas `/api` para `http://localhost:8080`. Se o backend estiver na porta 8081, defina o destino antes de iniciar o Vite:

```powershell
$env:VITE_API_TARGET = "http://localhost:8081"
npm run dev
```

Para validar e gerar a versão de produção, rode `npm run build` dentro de `frontend`.

## Endpoints

| Método | Caminho | Resultado |
| --- | --- | --- |
| GET | `/api/health` | Estado básico da aplicação |
| GET | `/api/nodes` | Lista Nodes |
| GET | `/api/nodes/{id}` | Busca Node por UUID |
| POST | `/api/nodes` | Cria Node |
| PUT | `/api/nodes/{id}/usage` | Atualiza a leitura atual de CPU e memória de um Node |
| DELETE | `/api/nodes/{id}` | Remove Node |
| GET | `/api/networks` | Lista Networks com IDs das Zones vinculadas |
| GET | `/api/networks/{id}` | Busca Network por UUID |
| GET | `/api/networks/{id}/metrics` | Resume Nodes, sobrecargas e médias de CPU e memória |
| POST | `/api/networks` | Cria Network |
| DELETE | `/api/networks/{id}` | Remove Network sem Zones vinculadas |
| GET | `/api/zones` | Lista Zones com IDs dos Nodes vinculados |
| GET | `/api/zones/{id}` | Busca Zone por UUID |
| GET | `/api/zones/{id}/metrics` | Resume Nodes, sobrecargas e médias de CPU e memória |
| POST | `/api/zones/{id}/distribution-plan` | Simula a distribuição de uma demanda entre Nodes ONLINE |
| POST | `/api/zones/{zoneId}/workloads` | Persiste uma demanda e sua alocação por Node |
| GET | `/api/zones/{zoneId}/workloads` | Lista demandas e seus estados/alocações |
| DELETE | `/api/workloads/{id}` | Encerra a demanda e libera a capacidade reservada |
| POST | `/api/zones` | Cria Zone |
| DELETE | `/api/zones/{id}` | Remove Zone sem Nodes vinculados |

Exemplo de criação:

```json
{
  "name": "edge-01",
  "cpuUsage": 35.5,
  "memoryUsage": 62.0,
  "zoneId": null
}
```

`zoneId` é opcional. Quando informado, precisa identificar uma Zone existente. Cada Node pertence a zero ou uma Zone, e cada Zone pode conter vários Nodes. CPU e memória aceitam valores de 0 a 100, inclusive. Novos Nodes começam com status `ONLINE`. Uma Zone com Nodes vinculados não pode ser removida; a API retorna HTTP 409. Erros de validação e recursos inexistentes são retornados como respostas HTTP com detalhes em formato Problem Details.

O endpoint `PUT /api/nodes/{id}/usage` recebe uma nova leitura de CPU e memória (ambas de 0 a 100), substitui os valores atuais e retorna o Node atualizado. Se CPU ou memória atingir seu limite, o status passa a `OVERLOADED`; se ambas ficarem abaixo dos limites, volta a `ONLINE`. Os limites padrão são 80% e podem ser alterados com `NODE_CPU_OVERLOAD_THRESHOLD` e `NODE_MEMORY_OVERLOAD_THRESHOLD`. Esta etapa registra leituras enviadas pelo cliente; não coleta métricas do sistema operacional. O Flyway aplica a migração versionada que atualiza a restrição de status no PostgreSQL sem remover Nodes existentes.

`POST /api/zones/{id}/distribution-plan` recebe `cpuDemand` e `memoryDemand` como pontos percentuais da capacidade de um Node e calcula uma divisão proporcional entre os Nodes `ONLINE` da Zone. A resposta informa se a demanda cabe na capacidade disponível e, quando couber, lista a parcela e a utilização projetada por Node. O cálculo usa os limites configurados e reserva 0,01 ponto percentual abaixo deles. É um plano de simulação: não altera Nodes, leituras nem estado no banco. Se não houver capacidade suficiente, retorna `feasible: false` e uma lista `allocations` vazia.

Para registrar uma demanda distribuída, envie `name`, `cpuDemand` e `memoryDemand` para `POST /api/zones/{zoneId}/workloads`. A API calcula e persiste as alocações somente se houver capacidade; caso contrário, responde HTTP 409 e não grava a demanda. Os valores são pontos percentuais totais da capacidade de um Node, divididos entre Nodes ONLINE. As demandas `ACTIVE` reservam capacidade em novos planos e criações. `DELETE /api/workloads/{id}` muda o estado para `STOPPED`, preserva a alocação como histórico e libera a reserva para cálculos futuros. A leitura atual de CPU/memória de cada Node continua sendo independente das demandas planejadas.

Exemplo de criação de demanda:

```json
{
  "name": "api-service",
  "cpuDemand": 30,
  "memoryDemand": 20
}
```

Exemplo de planejamento:

```json
{
  "cpuDemand": 30,
  "memoryDemand": 20
}
```

Uma Network pode conter várias Zones, e cada Zone pode pertencer a zero ou uma Network. `networkId` é opcional ao criar uma Zone; quando informado, precisa identificar uma Network existente. Networks com Zones vinculadas não podem ser removidas (HTTP 409).

Para criar a hierarquia, envie `{"name":"production"}` a `POST /api/networks`, use o UUID retornado como `networkId` em `POST /api/zones`, e use o UUID da Zone como `zoneId` em `POST /api/nodes`.

## Estrutura

O código Java está em `backend/src/main/java/com/dysonos/`, dividido em `controller`, `service`, `repository`, `model`, `dto` e `exception`. A configuração PostgreSQL usa variáveis de ambiente, e `docker-compose.yml` sobe um banco local para desenvolvimento.
