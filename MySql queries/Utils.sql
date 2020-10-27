-- gonnected Users
SELECT SUBSTRING_INDEX(host, ':', 1) AS host_short,
       GROUP_CONCAT(DISTINCT user) AS users,
       COUNT(*) AS threads
FROM information_schema.processlist
GROUP BY host_short
ORDER BY COUNT(*), host_short;
-- get charset
SELECT default_character_set_name FROM information_schema.SCHEMATA 
WHERE schema_name = "main";


delete from user where id != 9999;
insert into user(name,username,password_hash) values('yee','yeet','dwefiogr');
insert into user(name,username,password_hash) values('yee','yeeeet','dwefiogr');
insert into user(name,username,password_hash) values('yee','dorin','dwefiogr');
insert into user(name,username,password_hash,photo_path) values('yee','dodsrin','dwefiogr','gae');
insert into user(name,username,password_hash,photo_path) values('','dsar','5j7irtsx93n2jojxywz09zxecwctwrdqrzkvb8oo2w7drxzmup','');
select * from user;
select count(id) from user where username = 'dsar' and password_hash = '7irtsx93n2jojxywz09zxecwctwrdqrzkvb8oo2w7drxzmup';