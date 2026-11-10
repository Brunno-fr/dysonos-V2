# DYSONOS V2

DYSONOS V2 é um simulador de infraestrutura distribuída. Esta etapa entrega apenas a fundação inicial do backend para cadastro e consulta de Nodes.

## Tecnologias do backend

- Java 21 e Spring Boot 3.5
- Spring Web, Spring Data JPA e Spring Validation
- PostgreSQL
- Maven
- JUnit 5 e Mockito

## Requisitos

- JDK 21
- Maven 3.6.3 ou superior
- Docker com Docker Compose (ou PostgreSQL instalado localmente)

## Executar

Na raiz do projeto, defina a senha local fora dos arquivos versionados e inicie o PostgreSQL. No PowerShell:

```powershell
$env:DB_PASSWORD = "sua-senha-local"
docker compose up -d postgres
```

Em outro terminal, execute a aplicação:

```bash
cd backend
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8080`. Para executar os testes:

```bash
cd backend
mvn test
```

As configurações do banco são definidas pelas variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. `DB_PASSWORD` deve ser fornecida pelo ambiente e não é armazenada no repositório.

## Endpoints

| Método | Caminho | Resultado |
| --- | --- | --- |
| GET | `/api/health` | Estado básico da aplicação |
| GET | `/api/nodes` | Lista Nodes |
| GET | `/api/nodes/{id}` | Busca Node por UUID |
| POST | `/api/nodes` | Cria Node |
| DELETE | `/api/nodes/{id}` | Remove Node |

Exemplo de criação:

```json
{
  "name": "edge-01",
  "cpuUsage": 35.5,
  "memoryUsage": 62.0
}
```

CPU e memória aceitam valores de 0 a 100, inclusive. Novos Nodes começam com status `ONLINE`. Erros de validação e Nodes inexistentes são retornados como respostas HTTP com detalhes em formato Problem Details.

## Estrutura

O código Java está em `backend/src/main/java/com/dysonos/`, dividido em `controller`, `service`, `repository`, `model`, `dto` e `exception`. A configuração PostgreSQL usa variáveis de ambiente, e `docker-compose.yml` sobe um banco local para desenvolvimento.
