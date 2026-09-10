-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO products AS existing
USING (VALUES
(1,'QUICK-CASH-30','Quick Cash 30','Single-payment short term loan','DAYS',7,30,0.0000,'LUMP_SUM',NULL,'INDIVIDUAL_DUE_DATE',TRUE,CURRENT_TIMESTAMP),
(2,'INSTALLMENT-6M','Installment Loan 6 Months','Calendar-month installments','MONTHS',3,6,0.0000,'INSTALLMENTS',6,'INDIVIDUAL_DUE_DATE',TRUE,CURRENT_TIMESTAMP),
(3,'PAYDAY-CONSOLIDATED','Consolidated Advance','Customer billing-day based advance','DAYS',5,25,0.0000,'LUMP_SUM',NULL,'CONSOLIDATED',TRUE,CURRENT_TIMESTAMP)) AS seed (id, code, name, description, tenure_type, min_tenure_value, max_tenure_value, interest_rate, loan_structure_type, installment_count, billing_cycle_type, active, created_at)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, code, name, description, tenure_type, min_tenure_value, max_tenure_value, interest_rate, loan_structure_type, installment_count, billing_cycle_type, active, created_at)
VALUES (seed.id, seed.code, seed.name, seed.description, seed.tenure_type, seed.min_tenure_value, seed.max_tenure_value, seed.interest_rate, seed.loan_structure_type, seed.installment_count, seed.billing_cycle_type, seed.active, seed.created_at);
-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO fees AS existing
USING (VALUES
(1,1,'SERVICE_FEE','PERCENTAGE','ORIGINATION',2.5000,NULL,TRUE),(2,1,'LATE_FEE','FIXED','POST_DISBURSEMENT',500.0000,3,TRUE),(3,1,'LATE_FEE','FIXED','POST_DISBURSEMENT',1000.0000,7,TRUE),
(4,2,'SERVICE_FEE','FIXED','ORIGINATION',1000.0000,NULL,TRUE),(5,2,'SERVICE_FEE','PERCENTAGE','POST_DISBURSEMENT',0.5000,NULL,TRUE),(6,2,'DAILY_FEE','PERCENTAGE','POST_DISBURSEMENT',0.0500,NULL,TRUE),(7,2,'LATE_FEE','PERCENTAGE','POST_DISBURSEMENT',1.0000,5,TRUE),
(8,3,'SERVICE_FEE','PERCENTAGE','POST_DISBURSEMENT',1.5000,NULL,TRUE),(9,3,'LATE_FEE','FIXED','POST_DISBURSEMENT',300.0000,2,TRUE)) AS seed (id, product_id, fee_category, calculation_type, application_timing, amount, days_after_due, active)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, product_id, fee_category, calculation_type, application_timing, amount, days_after_due, active)
VALUES (seed.id, seed.product_id, seed.fee_category, seed.calculation_type, seed.application_timing, seed.amount, seed.days_after_due, seed.active);
-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO notification_templates AS existing
USING (VALUES
(1,'LOAN_CREATED_EMAIL','LOAN_CREATED','EMAIL','Loan created','Hi {customerName}, loan {loanNumber} was created.',TRUE),(2,'LOAN_CREATED_SMS','LOAN_CREATED','SMS',NULL,'Loan {loanNumber} was created.',TRUE),
(3,'LOAN_DISBURSED_EMAIL','LOAN_DISBURSED','EMAIL','Loan disbursed','Loan {loanNumber} was disbursed. Due {dueDate}.',TRUE),(4,'LOAN_DISBURSED_SMS','LOAN_DISBURSED','SMS',NULL,'Loan {loanNumber} disbursed. Due {dueDate}.',TRUE),
(5,'DUE_REMINDER_EMAIL','DUE_DATE_REMINDER','EMAIL','Payment due soon','Loan {loanNumber} is due on {dueDate}.',TRUE),(6,'DUE_REMINDER_SMS','DUE_DATE_REMINDER','SMS',NULL,'Loan {loanNumber} is due on {dueDate}.',TRUE),
(7,'REPAYMENT_EMAIL','REPAYMENT_ACKNOWLEDGED','EMAIL','Repayment received','Payment {amount} received for {loanNumber}.',TRUE),(8,'REPAYMENT_SMS','REPAYMENT_ACKNOWLEDGED','SMS',NULL,'Payment {amount} received for {loanNumber}.',TRUE),
(9,'OVERDUE_EMAIL','OVERDUE_NOTICE','EMAIL','Loan overdue','Loan {loanNumber} is overdue.',TRUE),(10,'OVERDUE_SMS','OVERDUE_NOTICE','SMS',NULL,'Loan {loanNumber} is overdue.',TRUE),
(11,'CLOSED_EMAIL','LOAN_CLOSED','EMAIL','Loan closed','Loan {loanNumber} is fully repaid.',TRUE),(12,'WRITTEN_OFF_EMAIL','LOAN_WRITTEN_OFF','EMAIL','Loan update','Loan {loanNumber} was written off.',TRUE)) AS seed (id, code, event_type, channel, subject_template, body_template, active)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, code, event_type, channel, subject_template, body_template, active)
VALUES (seed.id, seed.code, seed.event_type, seed.channel, seed.subject_template, seed.body_template, seed.active);
-- Preserve existing rows, including user edits and reserved lending limits.
MERGE INTO notification_rules AS existing
USING (VALUES
(1,'OVERDUE_NOTICE',NULL,'PREMIUM','EMAIL',TRUE),(2,'DUE_DATE_REMINDER',1,NULL,'SMS',TRUE),(3,'LOAN_DISBURSED',NULL,NULL,'SMS',TRUE)) AS seed (id, event_type, product_id, customer_segment, channel, enabled)
ON existing.id = seed.id
WHEN NOT MATCHED THEN INSERT (id, event_type, product_id, customer_segment, channel, enabled)
VALUES (seed.id, seed.event_type, seed.product_id, seed.customer_segment, seed.channel, seed.enabled);
