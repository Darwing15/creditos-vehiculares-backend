ALTER TABLE branches
ADD CONSTRAINT fk_branch_manager
FOREIGN KEY (manager_id)
REFERENCES users(id);