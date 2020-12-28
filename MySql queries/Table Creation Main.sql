-- change from main to other stuff to create more databases for the different servers to use
drop database if exists main;
create database if not exists main;
use main;

drop user 'server';
create user 'server'@'%' identified by 'VeryStrongPassword';
grant all privileges on main.* to 'server';

drop table if exists Channel_Message;
drop table if exists User_Message;
drop table if exists Channel_User;
drop table if exists Message;
drop table if exists Channel;
drop table if exists User;

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
    creator_id int not null,
    name varchar(64) not null unique,
    password_hash char(64) not null,
    description varchar(256) not null,
    creation_moment datetime not null default current_timestamp,

    foreign key (creator_id) references User(id) 
);
-- Channel_User ----------------------------------
create table if not exists Channel_User (
	id int not null unique auto_increment,
	channel_id int not null,
    user_id int not null,
    
    primary key (channel_id,user_id),
	foreign key (channel_id) references Channel(id) on delete cascade,
	foreign key (user_id) references User(id) on delete cascade
);
-- Message ----------------------------------
create table if not exists Message (
	id int not null primary key,
    sender_id int not null,
    moment_sent datetime not null default current_timestamp,
    type enum( 'text' , 'file' ) not null,
    content varchar(512) not null,
    
    foreign key (sender_id) references User(id)
);
-- User_Message ----------------------------------
create table if not exists User_Message(
	receiver_id int not null,
	message_id int not null unique,

	primary key (receiver_id, message_id),
	foreign key (receiver_id) references User(id),
	foreign key (message_id) references Message(id)
);
-- Channel_Message ----------------------------------
create table if not exists Channel_Message(
	channel_id int not null,
	message_id int not null unique,

	primary key (channel_id, message_id),
	foreign key (channel_id) references Channel(id) on delete cascade,
	foreign key (message_id) references Message(id) on delete cascade
);
