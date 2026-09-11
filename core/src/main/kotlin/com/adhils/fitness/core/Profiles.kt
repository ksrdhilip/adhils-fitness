package com.adhils.fitness.core

import kotlinx.serialization.Serializable

@Serializable data class ProfileRecord(val id: String = newId(), val state: AppState = AppState())
@Serializable data class ProfileStore(val version: Int = 1, val selectedId: String = "",
    val profiles: List<ProfileRecord> = emptyList()) {
    fun initialized(): ProfileStore = if (profiles.isEmpty()) {
        val initial = ProfileRecord()
        copy(selectedId = initial.id, profiles = listOf(initial))
    } else this
    val selected: ProfileRecord get() = profiles.first { it.id == selectedId }
    fun update(id: String = selectedId, change: (AppState) -> AppState): ProfileStore =
        copy(profiles = profiles.map { if (it.id == id) it.copy(state = change(it.state)) else it })
    fun select(id: String): ProfileStore {
        require(profiles.any { it.id == id }); return copy(selectedId = id)
    }
    fun add(name: String): ProfileStore {
        require(name.trim().isNotEmpty() && name.length <= 80 && profiles.size < 20)
        val record = ProfileRecord(state = AppState(profile = Profile(name = name.trim())))
        return copy(selectedId = record.id, profiles = profiles + record)
    }
    fun validated(): ProfileStore {
        require(version == 1 && profiles.size in 1..20)
        require(profiles.map { it.id }.distinct().size == profiles.size)
        require(profiles.any { it.id == selectedId })
        profiles.forEach { it.state.validated() }
        return this
    }
}
