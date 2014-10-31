import java.io.File

fun main(args: Array<String>) {
    val students = Unit("Students", "Student_Id", "Student_Name", "Group_Id")
    val groups = Unit("Groups", "Group_Id", "Group_Name")
    val schedule = Unit("Schedule", "Group_Id", "Course_Id", "Lecturer_Id")
    val lecturers = Unit("Lecturers", "Lecturer_Id", "Lecturer_Name")
    val courses = Unit("Courses", "Course_Id", "Course_Name")
    val marks = Unit("Marks", "Student_Id", "Course_Id", "Mark")

    println(LeftSemiJoin(
            students,
            Filter(
                    LeftSemiJoin(
                            marks,
                            Filter(courses, "Course_Name='Базы данных'")
                    ),
                    "Mark=5"
            )
    ).toSQL())

    println(
            LeftSemiJoin(
                    students,
                    LeftSemiJoin(
                            marks,
                            Filter(courses, "Course_Name='Базы данных'")
                    )
            ).toSQL()
    )

    println(
            LeftSemiJoin(
                    LeftSemiJoin(
                            students,
                            LeftSemiJoin(
                                    groups,
                                    LeftSemiJoin(
                                            schedule,
                                            courses
                                    )

                            )
                    ),
                    LeftSemiJoin(
                            marks,
                            Filter(courses, "Course_Name='Базы данных'")
                    )
            ).toSQL()
    )

    println(
            LeftSemiJoin(
                    students,
                    LeftSemiJoin(
                            marks,
                            LeftSemiJoin(
                                    courses,
                                    LeftSemiJoin(
                                            schedule,
                                            Filter(lecturers, "Lecturer_Id=1")
                                    )
                            )
                    )
            ).toSQL()
    )

    println(
            Projection (
                    Complement(
                            students,
                            LeftSemiJoin(
                                    students,
                                    LeftSemiJoin(
                                            marks,
                                            LeftSemiJoin(
                                                    courses,
                                                    LeftSemiJoin(
                                                            schedule,
                                                            Filter(lecturers, "Lecturer_Id=1")
                                                    )
                                            )
                                    )
                            )
                    ),
                    "Student_Id"
            ).toSQL()
    )

    println(
            LeftSemiJoin(
                    students,
                    Division(
                            marks,
                            Projection(
                                    LeftSemiJoin(
                                            courses,
                                            LeftSemiJoin(
                                                    schedule,
                                                    Filter(
                                                            lecturers,
                                                            "Lecturer_Id=1"
                                                    )
                                            )
                                    ),
                                    "Course_Id"
                            )
                    )
            ).toSQL()
    )

    println(
            Projection(
                    NaturalJoin(
                            students,
                            NaturalJoin(
                                    groups,
                                    NaturalJoin(schedule, courses)
                            )
                    ),
                    "Student_Name", "Course_Id"
            ).toSQL()
    )

    println(
            LeftSemiJoin(
                    students,
                    LeftSemiJoin(
                            groups,
                            LeftSemiJoin(
                                    schedule,
                                    Filter(
                                            lecturers,
                                            "Lecturer_Id=1"
                                    )
                            )
                    )
            ).toSQL()
    )

    val s = Projection(
            NaturalJoin(
                    students,
                    NaturalJoin(marks, courses)
            ),
            "Student_Id", "Course_Id"
    )
    println(
            BigDivision(
                    Rename(
                            s,
                            RenameEntry("Student_Id", "Student1")
                    ),
                    Rename(
                            s,
                            RenameEntry("Student_Id", "Student2")
                    )
            ).toSQL()
    )
}
