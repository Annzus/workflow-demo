alter table applications
    add column workflow_version_id uuid references workflow_versions(id);
