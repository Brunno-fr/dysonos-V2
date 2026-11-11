# Visão da arquitetura inicial

O backend usa camadas pequenas para manter cada responsabilidade clara:

- `controller`: recebe HTTP, valida o corpo da requisição e define os códigos de resposta.
- `service`: contém os casos de uso de consulta, criação e remoção.
- `repository`: usa Spring Data JPA para persistir Nodes, Zones e Networks.
- `model`: contém as entidades JPA `Node`, `Zone` e `Network`, além do enum `NodeStatus`.
- `dto`: separa os dados recebidos e enviados pela API da entidade persistida.
- `exception`: converte erros conhecidos em respostas HTTP Problem Details.

O PostgreSQL é configurado em `backend/src/main/resources/application.yml`. A URL e o usuário podem ser substituídos por `DB_URL` e `DB_USERNAME`; a senha é obrigatoriamente lida de `DB_PASSWORD`. O `docker-compose.yml` fornece um PostgreSQL para desenvolvimento local, sem gravar a senha no código.

Uma Network pode conter várias Zones, e uma Zone pode conter vários Nodes. Os vínculos Zone–Network e Node–Zone são opcionais para manter válidos os registros já existentes. A exclusão de Network ou Zone é recusada com HTTP 409 enquanto houver registros dependentes; nenhuma exclusão em cascata é usada.

O status de Node começa como `ONLINE` ao construir a entidade. `createdAt` recebe o instante UTC no ciclo de persistência. A validação da API limita os usos de CPU e memória ao intervalo inclusivo de 0 a 100.

O cliente pode enviar novas leituras de CPU e memória por `PUT /api/nodes/{id}/usage`. O serviço substitui os valores atuais e marca o Node como `OVERLOADED` quando CPU ou memória atinge o limite configurado (80% por padrão); quando ambos ficam abaixo, o status retorna a `ONLINE`. Os limites são configuráveis por `NODE_CPU_OVERLOAD_THRESHOLD` e `NODE_MEMORY_OVERLOAD_THRESHOLD`. As leituras são simuladas e fornecidas pelo cliente, não coletadas do sistema operacional.

Uma migração Flyway atualiza a restrição de status no PostgreSQL para permitir `ONLINE` e `OVERLOADED`. Em bancos já existentes, o Flyway cria a linha de base na versão 0 e aplica a migração V1; nenhum Node é removido.

O endpoint `GET /api/networks/{id}/metrics` agrega a quantidade de Zones e Nodes, Nodes sobrecarregados e as médias aritméticas das porcentagens de CPU e memória dos Nodes vinculados. Em Networks sem Nodes, as médias são retornadas como zero. Os valores representam as leituras simuladas atualmente armazenadas.

O endpoint `GET /api/zones/{id}/metrics` resume a quantidade de Nodes da Zone, Nodes sobrecarregados e as médias aritméticas atuais de CPU e memória, além do ID da Network associada quando houver.

O endpoint `POST /api/zones/{id}/distribution-plan` cria apenas um plano em memória para distribuir uma demanda proporcional de CPU e memória pelos Nodes `ONLINE` daquela Zone. O cálculo usa os limites configurados e preserva uma margem de 0,01 ponto percentual. Nodes sobrecarregados são excluídos. O plano não persiste nem aplica a alocação e não altera as leituras atuais. Quando a capacidade é insuficiente, a resposta informa `feasible: false` sem retornar alocações parciais.

Demandas persistidas usam `Workload` e `WorkloadAllocation`. O endpoint `POST /api/zones/{zoneId}/workloads` calcula a distribuição e grava demanda e alocações na mesma transação. Uma consulta agregada considera as alocações `ACTIVE` nos planos seguintes. O endpoint `DELETE /api/workloads/{id}` muda o estado para `STOPPED`, mantendo o histórico e deixando de reservar capacidade. As leituras de uso do Node não são sobrescritas. O Flyway V2 cria as tabelas, chaves estrangeiras, restrições e índices dessas entidades.

O endpoint `/api/health` informa apenas que a aplicação respondeu. Ele não verifica a conectividade do banco de dados.
