-- Vizitor schema — Microsoft SQL Server
-- Idempotent and non-destructive: only creates objects that are missing.
-- Never DROPs or ALTERs anything.
-- NOTE: statements are kept semicolon-free internally on purpose (loader splits on ';').

IF OBJECT_ID(N'dbo.users', N'U') IS NULL CREATE TABLE dbo.users (
    id INT IDENTITY(1,1) NOT NULL,
    username NVARCHAR(64) NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    is_admin BIT NOT NULL CONSTRAINT DF_users_is_admin DEFAULT(0),
    created_at DATETIME2 NOT NULL CONSTRAINT DF_users_created DEFAULT(SYSDATETIME()),
    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UQ_users_username UNIQUE (username)
);

IF OBJECT_ID(N'dbo.visitors', N'U') IS NULL CREATE TABLE dbo.visitors (
    id INT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(128) NOT NULL,
    phone NVARCHAR(32) NULL,
    purpose NVARCHAR(255) NULL,
    host_name NVARCHAR(128) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_visitors_created DEFAULT(SYSDATETIME()),
    CONSTRAINT PK_visitors PRIMARY KEY (id)
);

IF OBJECT_ID(N'dbo.sessions', N'U') IS NULL CREATE TABLE dbo.sessions (
    id INT IDENTITY(1,1) NOT NULL,
    token CHAR(64) NOT NULL,
    user_id INT NOT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_sessions_created DEFAULT(SYSDATETIME()),
    CONSTRAINT PK_sessions PRIMARY KEY (id),
    CONSTRAINT UQ_sessions_token UNIQUE (token),
    CONSTRAINT FK_sessions_user FOREIGN KEY (user_id) REFERENCES dbo.users (id) ON DELETE CASCADE
);

IF OBJECT_ID(N'dbo.settings', N'U') IS NULL CREATE TABLE dbo.settings (
    k NVARCHAR(64) NOT NULL,
    v NVARCHAR(MAX) NULL,
    CONSTRAINT PK_settings PRIMARY KEY (k)
);
