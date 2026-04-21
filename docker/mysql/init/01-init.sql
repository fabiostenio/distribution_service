-- Script de inicialização do banco de dados flowpay_db
-- Executado automaticamente pelo MySQL na primeira inicialização

CREATE DATABASE IF NOT EXISTS flowpay_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE flowpay_db;

-- Garante que o usuário tem todas as permissões no banco
GRANT ALL PRIVILEGES ON flowpay_db.* TO 'flowpay_user'@'%';
FLUSH PRIVILEGES;
