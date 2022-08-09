[![Lint](https://github.com/projectronin/interop-validation/actions/workflows/lint.yml/badge.svg)](https://github.com/projectronin/interop-validation/actions/workflows/lint.yml)

# interop-validation-liquibase

Provides Liquibase changelogs defining the Data Validation database.

## Conventions

### Indexes and Constraints ###

All names should be written in lowercase.

Note that there is a 64-character limit for MySQL; therefore, any examples that may exceed that limit should have
appropriate truncation or name simplification done such that they are still easy to follow.

#### Primary Key ####

pk_TABLE

#### Foreign Key ####

fk_TABLE_REFERENCEDTABLE

#### Unique ####

uk_TABLE_COLUMN uk_TABLE_COLUMN1_COLUMN2...
