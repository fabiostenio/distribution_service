.PHONY: up down build restart logs ps clean db-shell app-shell

## Sobe todos os containers (build se necessário)
up:
	docker compose up -d --build

## Sobe apenas o banco de dados
up-db:
	docker compose up -d db

## Build da imagem da aplicação sem subir
build:
	docker compose build app

## Derruba todos os containers
down:
	docker compose down

## Derruba e remove volumes (apaga dados do banco)
clean:
	docker compose down -v --remove-orphans

## Reinicia todos os containers
restart:
	docker compose restart

## Exibe logs em tempo real
logs:
	docker compose logs -f

## Logs apenas da aplicação
logs-app:
	docker compose logs -f app

## Logs apenas do banco
logs-db:
	docker compose logs -f db

## Status dos containers
ps:
	docker compose ps

## Abre shell no container do banco
db-shell:
	docker compose exec db mysql -u flowpay_user -pflowpay_pass flowpay_db

## Abre shell no container da aplicação
app-shell:
	docker compose exec app sh

## Verifica se a aplicação está respondendo
status:
	curl -s http://localhost:8080/api/status && echo
