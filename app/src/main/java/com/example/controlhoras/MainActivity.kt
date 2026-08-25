fun formatearHora(texto: String): String {
    val digitos = texto.filter { it.isDigit() }.take(4)
    return when (digitos.length) {
        0 -> ""
        1, 2, 3 -> digitos
        else -> "${digitos.substring(0, 2)}:${digitos.substring(2, 4)}"
    }
}
