import java.util.ArrayList

public abstract class Relation {
    public abstract val fields: List<String>
    public abstract fun toSQL(): String
}

private var maxAlias = 0

private fun subQuery(rel: Relation): String {
    if (rel is Unit) {
        return rel.toSQL()
    } else {
        maxAlias++
        val alias = "a$maxAlias"
        return "(${rel.toSQL()}) as $alias"
    }
}

public class Unit(val name: String, vararg fields: String) : Relation() {
    override val fields = fields.toArrayList()

    override fun toSQL(): String {
        return name
    }
}

public class Projection(public val what: Relation, vararg fields: String) : Relation() {
    override val fields = fields.toArrayList();
    {
        if (!what.fields.containsAll(this.fields)) {
            throw IllegalArgumentException("Illegal fields")
        }
    }

    override fun toSQL(): String {
        return "select ${fields.join(", ")} from ${subQuery(what)}"
    }
}

public class Filter(public val what: Relation, public val condition: String) : Relation() {
    override val fields = what.fields

    override fun toSQL(): String {
        return "select * from ${subQuery(what)} where $condition"
    }
}

public class RenameEntry(public val name: String, public val newName: String)

public class Rename(public val what: Relation, vararg val entries: RenameEntry) : Relation() {
    override val fields = ArrayList(what.fields);
    {
        for (entry in entries) {
            if (fields.contains(entry.name)) {
                fields.remove(entry.name)
                fields.add(entry.newName)
            } else {
                throw IllegalArgumentException("No such field: ${entry.name}")
            }
        }
    }

    override fun toSQL(): String {
        val sb = StringBuilder("select ")
        for (field in what.fields) {
            if (sb.length() != "select ".length) {
                sb.append(", ")
            }
            sb.append(field)
            val e = entries.firstOrNull { it.name == field }
            if (e != null) {
                sb.append(" as ${e.newName}")
            }
        }
        sb.append(" from ${subQuery(what)}")
        return sb.toString()
    }
}

class Union(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = op1.fields
    {
        if (!op1.fields.containsAll(op2.fields)) {
            throw IllegalArgumentException("Cannot apply set operations on relations with different fields")
        }
    }

    override fun toSQL(): String {
        return "select * from ${subQuery(op1)} union select * from ${subQuery(op2)}"
    }
}

class Intersection(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = op1.fields
    {
        if (!op1.fields.containsAll(op2.fields)) {
            throw IllegalArgumentException("Cannot apply set operations on relations with different fields")
        }
    }

    override fun toSQL(): String {
        return "select * from ${subQuery(op1)} intersect select * from ${subQuery(op2)}"
    }
}

class Complement(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = op1.fields
    {
        if (!op1.fields.containsAll(op2.fields)) {
            throw IllegalArgumentException("Cannot apply set operations on relations with different fields")
        }
    }

    override fun toSQL(): String {
        return "select * from ${subQuery(op1)} except select * from ${subQuery(op2)}"
    }
}

class NaturalJoin(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = ArrayList(op1.fields);
    {
        fields.addAll(op2.fields)
    }

    override fun toSQL(): String {
        return "select * from ${subQuery(op1)} natural join ${subQuery(op2)}"
    }
}

class CrossJoin(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = ArrayList(op1.fields);
    {
        fields.addAll(op2.fields)
    }

    override fun toSQL(): String {
        return "select * from ${subQuery(op1)} cross join ${subQuery(op2)}"
    }
}

class LeftJoin(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = ArrayList(op1.fields);
    {
        fields.removeAll(op2.fields)
    }

    override fun toSQL(): String {
        return "select * from ${subQuery(op1)} left join ${subQuery(op2)}"
    }
}

class RightJoin(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = ArrayList(op1.fields);
    {
        fields.removeAll(op2.fields)
    }

    override fun toSQL(): String {
        return "select * from ${subQuery(op1)} right join ${subQuery(op2)}"
    }
}

class LeftSemiJoin(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = op1.fields

    override fun toSQL(): String {
        // Help me unsee it!
        val fieldsArr = fields.toArrayList().toArray(array(""))
        return Projection(NaturalJoin(op1, op2), *fieldsArr).toSQL()
    }
}

class RightSemiJoin(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = op2.fields

    override fun toSQL(): String {
        val fieldsArr = fields.toArrayList().toArray(array(""))
        return Projection(NaturalJoin(op1, op2), *fieldsArr).toSQL()
    }
}

class Division(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = ArrayList(op1.fields);
    {
        if (!fields.containsAll(op2.fields)) {
            throw IllegalArgumentException("The divident doesn't contain some fields of the divisor")
        }
        fields.removeAll(op2.fields)
    }

    override fun toSQL(): String {
        val x = fields.toArray(array(""))
        return Complement(
                Projection(op1, *x),
                Projection(
                        Complement(
                                CrossJoin(
                                        Projection(op1, *x),
                                        op2
                                ),
                                op1
                        ),
                        *x
                )
        ).toSQL()
    }
}

class BigDivision(public val op1: Relation, public val op2: Relation): Relation() {
    override val fields = ArrayList<String>()
    private val x = ArrayList(op1.fields)
    private val y = ArrayList(op1.fields)
    private val z = ArrayList(op2.fields);
    {
        x.removeAll(op2.fields)
        y.removeAll(x)
        z.removeAll(y)
        fields.addAll(x)
        fields.addAll(z)
    }

    override fun toSQL(): String {
        val x = x.toArray(array(""))
        val z = z.toArray(array(""))
        val xz = fields.toArray(array(""))
        return Complement(
                CrossJoin(
                        Projection(op1, *x),
                        Projection(op2, *z)
                ),
                Projection(
                        Complement(
                                CrossJoin(Projection(op1, *x), op2),
                                NaturalJoin(op1, op2)
                        ),
                        *xz
                )
        ).toSQL()
    }
}
