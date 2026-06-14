-- 门票全链路 7 个服务各自独立 database（同 MySQL 实例），对应 docs/10-门票全链路数据库设计.md「一、通用约定」
-- 各服务通过 Flyway（db/migration/V1__init.sql）在各自 database 内建表，此处只负责建库。
CREATE DATABASE IF NOT EXISTS product_db             DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pricing_db             DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS member_db              DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db           DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db             DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ticket_verification_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db               DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
