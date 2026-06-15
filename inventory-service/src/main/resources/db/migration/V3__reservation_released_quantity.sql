-- 部分退票支持：预占记录上记录「已释放数量」，使一笔多张的预占可按张分次释放。
-- released_quantity < quantity 时预占仍处 HOLDING/CONFIRMED（部分释放）；相等时整笔 RELEASED。
ALTER TABLE inventory_reservation
  ADD COLUMN released_quantity INT NOT NULL DEFAULT 0 COMMENT '已释放数量（部分退票累加）' AFTER quantity;
