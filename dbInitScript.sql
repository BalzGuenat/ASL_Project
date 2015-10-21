DROP SCHEMA asl CASCADE;

CREATE SCHEMA asl;

CREATE TABLE asl.active_queues (id uuid PRIMARY KEY);

CREATE TABLE asl.active_clients (id uuid PRIMARY KEY);

CREATE TABLE asl.message (
	messageid uuid PRIMARY KEY,
	senderid uuid NOT NULL REFERENCES asl.active_clients(id),
	receiverid uuid REFERENCES asl.active_clients(id),
	queueid uuid NOT NULL REFERENCES asl.active_queues(id) ON DELETE CASCADE,
	timeofarrival timestamp NOT NULL,
	body text,
	delivered BOOL DEFAULT FALSE NOT NULL);

CREATE INDEX messageididx
ON asl.message
USING btree (messageid);

CREATE INDEX receiverididx
ON asl.message
USING btree (receiverid);

CREATE INDEX queueididx
ON asl.message
USING btree (queueid);