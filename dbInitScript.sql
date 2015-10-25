DROP SCHEMA IF EXISTS asl CASCADE;

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

CREATE INDEX timeofarrivelidx
ON asl.message
USING HASH (timeofarrival);

CREATE INDEX messageididx
ON asl.message (messageid);

CREATE INDEX receiverididx
ON asl.message (receiverid);

CREATE INDEX queueididx
ON asl.message (queueid);

CREATE FUNCTION peekQueue(aqueueid uuid, areceiverid uuid)
	RETURNS SETOF asl.message AS $$
		BEGIN
      RETURN QUERY
			SELECT * FROM asl.message
			WHERE (receiverid = areceiverid OR receiverid IS NULL) AND
						queueid = aqueueid
			ORDER BY timeofarrival ASC
			LIMIT 1;
		END;
	$$ LANGUAGE plpgsql;


CREATE FUNCTION popQueue(aqueueid uuid, areceiverid uuid)
	RETURNS SETOF asl.message AS $$
		BEGIN
      RETURN QUERY DELETE FROM asl.message
			WHERE messageid IN
				(SELECT messageid FROM asl.message
				WHERE (receiverid = areceiverid OR receiverid IS NULL) AND
							queueid = aqueueid
				ORDER BY timeofarrival ASC
				LIMIT 1)
			RETURNING *;
			RETURN;
		END;
	$$ LANGUAGE plpgsql;
