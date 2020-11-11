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
insert into user(id,name,username,password_hash) values(3,'yee','yeet','dwefiogr');
-- insert into user(name,username,password_hash) values('yee','yeeeet','dwefiogr');
-- insert into user(name,username,password_hash,photo_path) values('yee','dodsrin','dwefiogr','gae');
insert into user(id,name,username,password_hash,photo_path) values(100,'','rodrigo','5j7irtsx93n2jojxywz09zxecwctwrdqrzkvb8oo2w7drxzmup','');
-- insert test channels
insert into channel(id,creator_id,name,description,password_hash) values(15,100,'its free real estate','hmmmm','12fpfd2m99l1gsstg61m8o5f0s1y5nkftwo3hxw96pvizb1otr');
insert into channel(id,creator_id,name,description,password_hash) values(1,3,'abc ','hmmmm','4iigni6ki9sm8k9jp9a7t2h9qsxtf4slsrow9un29vbner8dlz');
-- insert test channel messages
insert into message(id,sender_id,content) values (1,100,'only you know what is gonna happen to you next'); 
insert into channel_message(channel_id,message_id) values (15,1);
insert into message(id,sender_id,content) values(2,100,'dont rely on others to make your life happen'); 
insert into channel_message(channel_id,message_id) values(15,2);
-- insert test between user messages

-- insert user to channel
insert into channel_user(channel_id,user_id) values(15,100);

update channel set password_hash = '12fpfd2m99l1gsstg61m8o5f0s1y5nkftwo3hxw96pvizb1otr' where id = 1;
update channel set name = ?, password_hash = ?, description = ? where id = ?;

-- Selects
select * from channel;
select * from user;
select * from channel_message;
select * from user_message;
select * from message;
select * from message,channel_message,channel where message_id = message.id and channel.id = channel_id and channel_id = 15;
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
and (sender_id = 100 and receiver_id = 2 or sender_id = 2 and receiver_id = 100 )
and id < 1000
order by moment_sent
limit 10;
-- delete all messages
delete from message where id != -1;

select max(id) from message,channel_message where message_id = id and channel_id = 1;

insert into channel_user() values(1,2);

select id,creator_id,name,description,(
	select count(*) from channel_user where channel_id = id and user_id = 2
) as is_part_of_channel
 from channel 
 order by name asc;

delete from channel where id = 17;
delete from channel_user where user_id = (select id from user where username =  'dorin');
delete from user where username = 'dorin';

insert into channel(creator_ir,name,description,password_hash) values();
 select count(id) from channel where id = 1 and creator_id = 4;
 
select * from user where photo_path like 'Asdfgtrwe123Asdfgtrwe123%';
 
select content from message where type = 'text' and content like 'filename%';

select id,(
	select sender_id from channel_message where message_id = message.id
)as sender_id,moment_sent,type,content
from message where (select channel_id from channel_message where channel_id = 3 and channel_message.message_id = message.id) = 2;

select * from user where username like '%channel%'; 

select count(id) from message,user_message where message.id = message_id && receiver_id = 2;
select id,name,username from user where username like '%%' order by (select count(id) from message,user_message where message.id = message_id && receiver_id = user.id), username limit 30;
