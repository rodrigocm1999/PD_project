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


-- insert test users
insert into user(name,username,password_hash) values('yee','yeet','dwefiogr');
insert into user(name,username,password_hash) values('yee','yeeeet','dwefiogr');
insert into user(name,username,password_hash,photo_path) values('yee','dodsrin','dwefiogr','gae');
insert into user(id,name,username,password_hash,photo_path) values(100,'','rodrigo','5j7irtsx93n2jojxywz09zxecwctwrdqrzkvb8oo2w7drxzmup','');
-- insert test channels
insert into channel(id,creator_id,name,description,password_hash) values(15,100,'its free real estate','hmmmm','dsa');
insert into channel(id,creator_id,name,description,password_hash) values(1,3,'abc ','hmmmm','dsa');
-- insert test channel messages
insert into message(id,sender_id,content) values(1,100,'only you know what is gonna happen to you next'); 
insert into channel_message(channel_id,message_id) values(15,1);
insert into message(id,sender_id,content) values(2,100,'dont rely on others to make your life happen'); 
insert into channel_message(channel_id,message_id) values(15,2);
-- insert test between user messages
insert into user_message();

select * from channel;
select * from user;
select * from message;
-- get all user messages to channels
select * from message where id in (select message_id from channel_message where sender_id = 100);
-- get all messages before certain one on channel
select id,type,content,moment_sent, sender_id
from message,channel_message
where message.id = channel_message.message_id
and channel_id = 15
and id < 10
order by moment_sent
limit 10;
-- get all messages before certain one between users
select id,type,content,moment_sent, sender_id
from message,user_message
where message.id = user_message.message_id
and (sender_id = 100 or receiver_id = 100)
and id < 6
order by moment_sent
limit 10;

select max(id) from message,channel_message where message_id = id and channel_id = 1;


insert into channel_user() values(1,2);

select id,creator_id,name,description,(
	select count(*) from channel_user where channel_id = id and user_id = 2
) as is_part_of_channel
 from channel 
 order by name asc;

delete from channel_user where user_id = (select id from user where username =  'dorin');
delete from user where username = 'dorin';

select * from channel;
select * from user;
insert into channel(creator_ir,name,description,password_hash) values();
 select count(id) from channel where id = 1 and creator_id = 4;
 
select id,(
	select sender_id from channel_message where message_id = message.id
)as sender_id,moment_sent,type,content
from message where (select channel_id from channel_message where channel_id = 3 and channel_message.message_id = message.id) = 2;
 
 