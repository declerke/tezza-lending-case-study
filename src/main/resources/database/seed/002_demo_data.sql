-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO customers AS existing
USING (VALUES
(1,'Amina','Otieno','amina.otieno@example.com','+254700111222','ID-10001','Nairobi','RETAIL','SMS','INDIVIDUAL_DUE_DATE',NULL,TRUE,CURRENT_TIMESTAMP),
(2,'Brian','Kiprop','brian.kiprop@example.com','+254700333444','ID-10002','Mombasa','PREMIUM','EMAIL','CONSOLIDATED',25,TRUE,CURRENT_TIMESTAMP),
(3,'Grace','Wanjiku','grace.wanjiku@example.com','+254700555666','ID-10003','Nakuru','RETAIL','PUSH','INDIVIDUAL_DUE_DATE',NULL,TRUE,CURRENT_TIMESTAMP)) AS seed (id, first_name, last_name, email, phone, national_id, address, customer_segment, preferred_channel, billing_cycle_type, billing_day, active, created_at)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, first_name, last_name, email, phone, national_id, address, customer_segment, preferred_channel, billing_cycle_type, billing_day, active, created_at)
VALUES (seed.id, seed.first_name, seed.last_name, seed.email, seed.phone, seed.national_id, seed.address, seed.customer_segment, seed.preferred_channel, seed.billing_cycle_type, seed.billing_day, seed.active, seed.created_at);
-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO loan_limits AS existing
USING (VALUES
(1,1,100000.0000,59000.0000,'STANDARD',CURRENT_TIMESTAMP,0),(2,2,500000.0000,465000.0000,'LOW_RISK',CURRENT_TIMESTAMP,0),(3,3,50000.0000,50000.0000,'STANDARD',CURRENT_TIMESTAMP,0)) AS seed (id, customer_id, max_limit, available_limit, risk_tier, updated_at, version)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, customer_id, max_limit, available_limit, risk_tier, updated_at, version)
VALUES (seed.id, seed.customer_id, seed.max_limit, seed.available_limit, seed.risk_tier, seed.updated_at, seed.version);
-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO loans AS existing
USING (VALUES
(1,'DEMO-PENDING-001',1,1,5000.0000,0.0000,30,'LUMP_SUM','INDIVIDUAL_DUE_DATE','PENDING',NULL,NULL,5000.0000,125.0000,CURRENT_TIMESTAMP,NULL),
(2,'DEMO-OPEN-001',1,1,10000.0000,0.0000,30,'LUMP_SUM','INDIVIDUAL_DUE_DATE','OPEN',CURRENT_DATE,DATEADD('DAY',25,CURRENT_DATE),10000.0000,250.0000,CURRENT_TIMESTAMP,NULL),
(3,'DEMO-INSTALL-001',1,2,24000.0000,0.0000,6,'INSTALLMENTS','INDIVIDUAL_DUE_DATE','OPEN',DATEADD('MONTH',-1,CURRENT_DATE),DATEADD('MONTH',5,CURRENT_DATE),20000.0000,1000.0000,CURRENT_TIMESTAMP,NULL),
(4,'DEMO-CLOSED-001',1,1,8000.0000,0.0000,30,'LUMP_SUM','INDIVIDUAL_DUE_DATE','CLOSED',DATEADD('DAY',-40,CURRENT_DATE),DATEADD('DAY',-10,CURRENT_DATE),0.0000,0.0000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
(5,'DEMO-OVERDUE-001',1,1,12000.0000,0.0000,30,'LUMP_SUM','INDIVIDUAL_DUE_DATE','OVERDUE',DATEADD('DAY',-12,CURRENT_DATE),DATEADD('DAY',-5,CURRENT_DATE),12000.0000,1500.0000,CURRENT_TIMESTAMP,NULL),
(6,'DEMO-CANCELLED-001',1,1,3000.0000,0.0000,30,'LUMP_SUM','INDIVIDUAL_DUE_DATE','CANCELLED',NULL,NULL,3000.0000,75.0000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
(7,'DEMO-CONSOLIDATED-A',2,3,15000.0000,0.0000,15,'LUMP_SUM','CONSOLIDATED','OPEN',CURRENT_DATE,DATEADD('DAY',17,CURRENT_DATE),15000.0000,225.0000,CURRENT_TIMESTAMP,NULL),
(8,'DEMO-CONSOLIDATED-B',2,3,20000.0000,0.0000,20,'LUMP_SUM','CONSOLIDATED','OPEN',CURRENT_DATE,DATEADD('DAY',17,CURRENT_DATE),20000.0000,300.0000,CURRENT_TIMESTAMP,NULL),
(9,'DEMO-WRITTEN-OFF',3,1,7000.0000,0.0000,30,'LUMP_SUM','INDIVIDUAL_DUE_DATE','WRITTEN_OFF',DATEADD('DAY',-60,CURRENT_DATE),DATEADD('DAY',-30,CURRENT_DATE),7000.0000,175.0000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)) AS seed (id, loan_number, customer_id, product_id, principal_amount, interest_rate, tenure_value, loan_structure_type, billing_cycle_type, state, disbursement_date, due_date, outstanding_principal, outstanding_fees, created_at, closed_at)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, loan_number, customer_id, product_id, principal_amount, interest_rate, tenure_value, loan_structure_type, billing_cycle_type, state, disbursement_date, due_date, outstanding_principal, outstanding_fees, created_at, closed_at)
VALUES (seed.id, seed.loan_number, seed.customer_id, seed.product_id, seed.principal_amount, seed.interest_rate, seed.tenure_value, seed.loan_structure_type, seed.billing_cycle_type, seed.state, seed.disbursement_date, seed.due_date, seed.outstanding_principal, seed.outstanding_fees, seed.created_at, seed.closed_at);
-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO installments AS existing
USING (VALUES
(1,2,1,DATEADD('DAY',25,CURRENT_DATE),10000.0000,0.0000,0.0000,0.0000,'PENDING'),
(2,3,1,DATEADD('MONTH',0,DATEADD('MONTH',-1,CURRENT_DATE)),4000.0000,0.0000,4000.0000,0.0000,'PAID'),
(3,3,2,DATEADD('MONTH',1,DATEADD('MONTH',-1,CURRENT_DATE)),4000.0000,0.0000,0.0000,0.0000,'PENDING'),
(4,3,3,DATEADD('MONTH',2,DATEADD('MONTH',-1,CURRENT_DATE)),4000.0000,0.0000,0.0000,0.0000,'PENDING'),
(5,3,4,DATEADD('MONTH',3,DATEADD('MONTH',-1,CURRENT_DATE)),4000.0000,0.0000,0.0000,0.0000,'PENDING'),
(6,3,5,DATEADD('MONTH',4,DATEADD('MONTH',-1,CURRENT_DATE)),4000.0000,0.0000,0.0000,0.0000,'PENDING'),
(7,3,6,DATEADD('MONTH',5,DATEADD('MONTH',-1,CURRENT_DATE)),4000.0000,0.0000,0.0000,0.0000,'PENDING'),
(8,4,1,DATEADD('DAY',-10,CURRENT_DATE),8000.0000,0.0000,8000.0000,0.0000,'PAID'),
(9,5,1,DATEADD('DAY',-5,CURRENT_DATE),12000.0000,1500.0000,0.0000,0.0000,'OVERDUE'),
(10,6,1,DATEADD('DAY',30,CURRENT_DATE),3000.0000,0.0000,0.0000,0.0000,'PENDING'),
(11,7,1,DATEADD('DAY',17,CURRENT_DATE),15000.0000,0.0000,0.0000,0.0000,'PENDING'),
(12,8,1,DATEADD('DAY',17,CURRENT_DATE),20000.0000,0.0000,0.0000,0.0000,'PENDING'),
(13,9,1,DATEADD('DAY',-30,CURRENT_DATE),7000.0000,0.0000,0.0000,0.0000,'OVERDUE')) AS seed (id, loan_id, installment_number, due_date, principal_due, fees_due, amount_paid, fees_paid, state)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, loan_id, installment_number, due_date, principal_due, fees_due, amount_paid, fees_paid, state)
VALUES (seed.id, seed.loan_id, seed.installment_number, seed.due_date, seed.principal_due, seed.fees_due, seed.amount_paid, seed.fees_paid, seed.state);
-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO loan_charges AS existing
USING (VALUES
(1,2,NULL,1,'SERVICE_FEE',250.0000,CURRENT_DATE,'Origination service fee','DEMO-OPEN-001:ORIGINATION:1'),
(2,5,9,2,'LATE_FEE',500.0000,DATEADD('DAY',-2,CURRENT_DATE),'Late fee tier 3 days','DEMO-OVERDUE-001:LATE:1:2'),
(3,5,9,3,'LATE_FEE',1000.0000,CURRENT_DATE,'Late fee tier 7 days','DEMO-OVERDUE-001:LATE:1:3')) AS seed (id, loan_id, installment_id, fee_configuration_id, fee_category, amount, applied_date, description, deduplication_key)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, loan_id, installment_id, fee_configuration_id, fee_category, amount, applied_date, description, deduplication_key)
VALUES (seed.id, seed.loan_id, seed.installment_id, seed.fee_configuration_id, seed.fee_category, seed.amount, seed.applied_date, seed.description, seed.deduplication_key);
-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO repayments AS existing
USING (VALUES
(1,4,'DEMO-REPAY-001',8000.0000,0.0000,8000.0000,DATEADD('DAY',-10,CURRENT_TIMESTAMP)),
(2,3,'DEMO-REPAY-002',4000.0000,0.0000,4000.0000,DATEADD('DAY',-2,CURRENT_TIMESTAMP))) AS seed (id, loan_id, reference, amount, allocated_to_fees, allocated_to_principal, paid_at)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, loan_id, reference, amount, allocated_to_fees, allocated_to_principal, paid_at)
VALUES (seed.id, seed.loan_id, seed.reference, seed.amount, seed.allocated_to_fees, seed.allocated_to_principal, seed.paid_at);
ALTER TABLE products ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM products);
ALTER TABLE fees ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM fees);
ALTER TABLE customers ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM customers);
ALTER TABLE loan_limits ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM loan_limits);
ALTER TABLE loans ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM loans);
ALTER TABLE installments ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM installments);
ALTER TABLE loan_charges ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM loan_charges);
ALTER TABLE repayments ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM repayments);
ALTER TABLE notification_templates ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM notification_templates);
ALTER TABLE notification_rules ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM notification_rules);
ALTER TABLE notification_logs ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id),999)+1 FROM notification_logs);
