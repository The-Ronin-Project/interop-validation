package com.projectronin.interop.validation.server.data.binding

import com.github.f4b6a3.uuid.UuidCreator
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Reads a UUID from a BINARY field.
 */
fun BaseTable<*>.binaryUuid(name: String): Column<UUID> = registerColumn(name, BinaryUUIDSqlType)

/**
 * SqlType supporting storing a UUID as a BINARY datatype.
 */
object BinaryUUIDSqlType : SqlType<UUID>(Types.BINARY, typeName = "binary") {
    override fun doGetResult(rs: ResultSet, index: Int): UUID? {
        val byteArray = rs.getBytes(index)
        return byteArray?.let { UuidCreator.fromBytes(byteArray) }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: UUID) {
        ps.setObject(index, UuidCreator.toBytes(parameter))
    }
}

/**
 * Reads a time into a UTC-based OffsetDateTime.
 */
fun BaseTable<*>.utcDateTime(name: String): Column<OffsetDateTime> = registerColumn(name, UTCDateTimeSqlType)

/**
 * SqlType supporting storing an OffsetDateTime relative to UTC.
 */
object UTCDateTimeSqlType : SqlType<OffsetDateTime>(Types.TIMESTAMP, "datetime") {
    override fun doGetResult(rs: ResultSet, index: Int): OffsetDateTime? {
        val timestamp = rs.getTimestamp(index)
        return timestamp?.let {
            OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC)
        }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: OffsetDateTime) {
        ps.setTimestamp(index, Timestamp.from(parameter.toInstant()))
    }
}
