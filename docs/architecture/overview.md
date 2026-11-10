# Visão da arquitetura inicial

O backend usa camadas pequenas para manter cada responsabilidade clara:

- `controller`: recebe HTTP, valida o corpo da requisição e define os códigos de resposta.
- `service`: contém os casos de uso de consulta, criação e remoção.
- `repository`: usa Spring Data JPA para persistir Nodes.
- `model`: contém a entidade JPA `Node` e o enum `NodeStatus`.
- `dto`: separa os dados recebidos e enviados pela API da entidade persistida.
- `exception`: converte erros conhecidos em respostas HTTP Problem Details.

O PostgreSQL é configurado em `backend/src/main/resources/application.yml`. A URL e o usuário podem ser substituídos por `DB_URL` e `DB_USERNAME`; a senha é obrigatoriamente lida de `DB_PASSWORD`. O `docker-compose.yml` fornece um PostgreSQL para desenvolvimento local, sem gravar a senha no código.

O status começa como `ONLINE` ao construir uma entidade. `createdAt` recebe o instante UTC no ciclo de persistência. A validação da API limita os usos de CPU e memória ao intervalo inclusivo de 0 a 100.

O endpoint `/api/health` informa apenas que a aplicação respondeu. Ele não verifica a conectividade do banco de dados.
