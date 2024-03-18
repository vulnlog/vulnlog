val v010 = Version(0, 1, 0)
val v100 = Version(1, 0, 0)

val r1 = release { name = "release 1"; upComing = v100; published { +v010 } }

branches { -r1 }
