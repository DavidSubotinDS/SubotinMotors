-- Insert initial data into tb_user
INSERT INTO tb_user (username, password, role) VALUES
('admin123', '$2a$12$iDYDnAGZRFOEI0c7DzskH.ErWVIuWuQm.NhTMcs49oheFXr1c27gS', 'ROLE_ADMIN'),
('user123', '$2a$10$ZJWpHtDtXsuGgW2Lj.0v6.QBuKX6q2PYp4RRd4483jJIvKltX4/gS', 'ROLE_USER');

-- Insert initial data into tb_role
INSERT INTO tb_role (role, id_user) VALUES
('ROLE_ADMIN', (SELECT id_user FROM tb_user WHERE username = 'admin123')),
('ROLE_USER', (SELECT id_user FROM tb_user WHERE username = 'user123'));

-- Insert initial data into tb_user_profile
INSERT INTO tb_user_profile (first_name, last_name, phone_number, address, about, id_user) VALUES
('Admin', 'Admin', '0123456789', 'Novi Sad', 'Admin about', (SELECT id_user FROM tb_user WHERE username = 'admin123')),
('User', 'User', '0123456789', 'Novi Sad', 'User about', (SELECT id_user FROM tb_user WHERE username = 'user123'));
