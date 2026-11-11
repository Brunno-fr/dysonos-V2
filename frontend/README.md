# DYSONOS V2 — Frontend

Painel React responsivo para consultar e operar a infraestrutura existente no backend Spring Boot.

## Requisitos

- Node.js 20 ou superior
- npm
- Backend DYSONOS ativo

## Desenvolvimento

Na raiz do repositório, em um terminal com o backend rodando na porta 8080:

```powershell
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. O proxy do Vite encaminha `/api` para `http://localhost:8080`.

Se o backend estiver na porta 8081, configure o destino do proxy antes de iniciar:

```powershell
$env:VITE_API_TARGET = "http://localhost:8081"
npm run dev
```

## Compilação

```powershell
npm run build
```

## Dados e limites atuais

- Redes, zonas, nós, métricas, planos de distribuição e demandas vêm dos endpoints implementados no backend.
- Gráficos acumulam amostras reais retornadas pela API enquanto a página está aberta; não há retenção histórica no servidor.
- Eventos exibidos são ações desta sessão do navegador.
- Expansão automática e recursos de segurança estão marcados como pendentes, pois ainda não existem endpoints correspondentes.
- A visualização 3D apresenta somente zonas e nós retornados pela API. Ela não representa a localização geográfica real dos recursos.
