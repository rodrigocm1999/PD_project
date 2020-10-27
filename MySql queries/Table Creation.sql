drop table if exists Channel_Message;
drop table if exists User_Message;
drop table if exists Channel_User;
drop table if exists Message;
drop table if exists Channel;
drop table if exists User;

create database if not exists main;
-- User ----------------------------------
create table if not exists User (
	id  int not null primary key auto_increment,
	name varchar(50) not null,
	username varchar(25) not null unique key,
	password_hash char(64) not null,
	photo_path varchar(128) null,
	user_creation datetime not null default current_timestamp
);
-- Channel ----------------------------------
create table if not exists Channel (
    id int NOT NULL primary key auto_increment,
    creator_id int NOT NULL,
    name varchar(64) NOT NULL,
    password_hash char(64) NOT NULL,
    description varchar(256) NULL,
    creation_moment datetime not null default current_timestamp,

    foreign key (creator_id) references User(id) 
);
-- Channel_User ----------------------------------
create table if not exists Channel_User (
	channel_id int not null,
    user_id int not null,
    
    primary key (channel_id,user_id),
	foreign key (channel_id) references Channel(id),
	foreign key (user_id) references User(id) 
);
-- Message ----------------------------------
create table if not exists Message (
	id int not null primary key,
    moment_sent datetime not null default current_timestamp,
    type enum( 'text' , 'file' ) not null,
    content varchar(2000) not null
);
-- User_Message ----------------------------------
create table if not exists User_Message(
	sender_id int not null,
	reciever_id int not null,
	message_id int not null,

	primary key (sender_id, reciever_id, message_id),
	foreign key (sender_id) references User(id),
	foreign key (reciever_id) references User(id),
	foreign key (message_id) references Message(id)
);
-- Channel_Message ----------------------------------
create table if not exists Channel_Message(
	sender_id int not null,
	channel_id int not null,
	message_id int not null,

	primary key (sender_id, channel_id, message_id),
	foreign key (sender_id) references User(id),
	foreign key (channel_id) references Channel(id),
	foreign key (message_id) references Message(id)
);
