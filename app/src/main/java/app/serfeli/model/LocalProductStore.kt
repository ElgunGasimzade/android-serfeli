package app.serfeli.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object LocalProductStore {
    var selected: Product? by mutableStateOf(null)
}
