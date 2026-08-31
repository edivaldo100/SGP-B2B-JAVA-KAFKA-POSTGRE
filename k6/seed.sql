-- Parceiros de teste com limite de crédito para o K6
INSERT INTO partner_credit (id, partner_id, credit_limit, available_credit, version)
VALUES
  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 1000000.00, 1000000.00, 0),
  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', 1000000.00, 1000000.00, 0),
  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000003', 1000000.00, 1000000.00, 0),
  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000004', 1000000.00, 1000000.00, 0),
  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000005', 1000000.00, 1000000.00, 0)
ON CONFLICT DO NOTHING;
