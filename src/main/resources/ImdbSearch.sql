-- SELECT sql FROM sqlite_schema 
CREATE TABLE films (
        id integer primary key not null,
        title varchar(255) not null,
        imdb_id varchar(255) not null,
        year varchar(25) not null,
        kind varchar(8) not null,
        rating double not null,
        cover_url text,
        votes text,
        runtimeHM varchar(6),
        countries text,
        genres text,
        is_on_drive boolean DEFAULT false
);
CREATE UNIQUE INDEX idx_imdb_id on films(imdb_id);