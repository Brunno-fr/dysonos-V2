# DYSONOS V2

**Autonomous Network Infrastructure**

DYSONOS V2 é um simulador inteligente de infraestrutura de redes distribuídas. O backend permite cadastrar Zones e Nodes e associar Nodes a uma Zone.

## Tecnologias

### Backend implementado

- Java 21 e Spring Boot 3.5
- Spring Web, Spring Data JPA e Spring Validation
- PostgreSQL
- Maven
- JUnit 5 e Mockito

### Frontend planejado

- React e TypeScript
- Vite, Tailwind CSS e Three.js

O frontend ainda não foi implementado.

## Status

Backend inicial de Nodes e Zones implementado e validado. O projeto segue em desenvolvimento.

## Requisitos

- JDK 21
- Maven 3.6.3 ou superior
- Docker com Docker Compose (ou PostgreSQL instalado localmente)

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

As configurações do banco são definidas pelas variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. A senha é fornecida pelo ambiente e não é armazenada no repositório.

## Endpoints

| Método | Caminho | Resultado |
| --- | --- | --- |
| GET | `/api/health` | Estado básico da aplicação |
| GET | `/api/nodes` | Lista Nodes |
| GET | `/api/nodes/{id}` | Busca Node por UUID |
| POST | `/api/nodes` | Cria Node |
| DELETE | `/api/nodes/{id}` | Remove Node |
| GET | `/api/zones` | Lista Zones com IDs dos Nodes vinculados |
| GET | `/api/zones/{id}` | Busca Zone por UUID |
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

Para criar uma Zone, envie `{"name":"south-dc"}` a `POST /api/zones` e use o UUID retornado como `zoneId` ao criar um Node.

## Estrutura

O código Java está em `backend/src/main/java/com/dysonos/`, dividido em `controller`, `service`, `repository`, `model`, `dto` e `exception`. A configuração PostgreSQL usa variáveis de ambiente, e `docker-compose.yml` sobe um banco local para desenvolvimento.
