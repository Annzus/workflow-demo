-- Development-only maintenance script.
-- Keeps master data and form definitions, then removes generated application data.
-- Prefer reset-demo-data.ps1 for local resets so MinIO attachment objects are removed too.

delete from application_attachments;
delete from approval_histories;
delete from approval_tasks;
delete from application_field_values;
delete from applications;
