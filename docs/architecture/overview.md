# Visão da arquitetura inicial

O backend usa camadas pequenas para manter cada responsabilidade clara:

- `controller`: recebe HTTP, valida o corpo da requisição e define os códigos de resposta.
- `service`: contém os casos de uso de consulta, criação e remoção.
- `repository`: usa Spring Data JPA para persistir Nodes e Zones.
- `model`: contém as entidades JPA `Node` e `Zone`, além do enum `NodeStatus`.
- `dto`: separa os dados recebidos e enviados pela API da entidade persistida.
- `exception`: converte erros conhecidos em respostas HTTP Problem Details.

O PostgreSQL é configurado em `backend/src/main/resources/application.yml`. A URL e o usuário podem ser substituídos por `DB_URL` e `DB_USERNAME`; a senha é obrigatoriamente lida de `DB_PASSWORD`. O `docker-compose.yml` fornece um PostgreSQL para desenvolvimento local, sem gravar a senha no código.

Uma Zone pode conter vários Nodes. Cada Node pode pertencer a zero ou uma Zone; o vínculo é opcional para manter válidos os Nodes já existentes. A Zone não remove Nodes em cascata, e sua exclusão é recusada com HTTP 409 enquanto houver Nodes vinculados.

O status de Node começa como `ONLINE` ao construir a entidade. `createdAt` recebe o instante UTC no ciclo de persistência. A validação da API limita os usos de CPU e memória ao intervalo inclusivo de 0 a 100.

O endpoint `/api/health` informa apenas que a aplicação respondeu. Ele não verifica a conectividade do banco de dados.
