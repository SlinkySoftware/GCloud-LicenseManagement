-- 
--   gcloudlicensemanagement - mssql-0.0.1.sql
--
--   Copyright (c) 2022-2023, Slinky Software
--
--   This program is free software: you can redistribute it and/or modify
--   it under the terms of the GNU Affero General Public License as
--   published by the Free Software Foundation, either version 3 of the
--   License, or (at your option) any later version.
--
--   This program is distributed in the hope that it will be useful,
--   but WITHOUT ANY WARRANTY; without even the implied warranty of
--   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--   GNU Affero General Public License for more details.
--
--   A copy of the GNU Affero General Public License is located in the 
--   AGPL-3.0.md supplied with the source code.
--
-- 
--
-- Author:  Michael Junek (michael@juneks.com.au)
-- Created: 17 Apr 2023
-- 

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

------------------------------------
-- SPRING SESSION TABLES REQUIRED --
------------------------------------

CREATE TABLE [LIC_SPRING_SESSION](
	[PRIMARY_ID] [char](36) NOT NULL,
	[SESSION_ID] [char](36) NOT NULL,
	[CREATION_TIME] [bigint] NOT NULL,
	[LAST_ACCESS_TIME] [bigint] NOT NULL,
	[MAX_INACTIVE_INTERVAL] [int] NOT NULL,
	[EXPIRY_TIME] [bigint] NOT NULL,
	[PRINCIPAL_NAME] [varchar](100) NULL,
 CONSTRAINT [LIC_SPRING_SESSION_PK] PRIMARY KEY CLUSTERED 
(
	[PRIMARY_ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) 
) 
GO

CREATE TABLE [LIC_SPRING_SESSION_ATTRIBUTES](
	[SESSION_PRIMARY_ID] [char](36) NOT NULL,
	[ATTRIBUTE_NAME] [varchar](200) NOT NULL,
	[ATTRIBUTE_BYTES] [image] NOT NULL,
 CONSTRAINT [LIC_SPRING_SESSION_ATTRIBUTES_PK] PRIMARY KEY CLUSTERED 
(
	[SESSION_PRIMARY_ID] ASC,
	[ATTRIBUTE_NAME] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) 
) 
GO
ALTER TABLE [LIC_SPRING_SESSION_ATTRIBUTES]  WITH CHECK ADD  CONSTRAINT [LIC_SPRING_SESSION_ATTRIBUTES_FK] FOREIGN KEY([SESSION_PRIMARY_ID]) REFERENCES [LIC_SPRING_SESSION] ([PRIMARY_ID]) ON DELETE CASCADE
GO
ALTER TABLE [LIC_SPRING_SESSION_ATTRIBUTES] CHECK CONSTRAINT [LIC_SPRING_SESSION_ATTRIBUTES_FK]
GO

-------------------------------
-- END SPRING SESSION TABLES --
-------------------------------

