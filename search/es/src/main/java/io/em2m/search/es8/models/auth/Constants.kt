package io.em2m.search.es8.models.auth

// Refs an undocumented endpoint: /_security/privilege/_builtin
val CLUSTER_PRIVILEGES = setOf(
    "all",
    "cancel_task",
    "create_snapshot",
    "cross_cluster_replication",
    "cross_cluster_search",
    "delegate_pki",
    "grant_api_key",
    "manage",
    "manage_api_key",
    "manage_autoscaling",
    "manage_behavioral_analytics",
    "manage_ccr",
    "manage_connector",
    "manage_data_frame_transforms",
    "manage_data_stream_global_retention",
    "manage_enrich",
    "manage_ilm",
    "manage_index_templates",
    "manage_inference",
    "manage_ingest_pipelines",
    "manage_logstash_pipelines",
    "manage_ml",
    "manage_oidc",
    "manage_own_api_key",
    "manage_pipeline",
    "manage_rollup",
    "manage_saml",
    "manage_search_application",
    "manage_search_query_rules",
    "manage_search_synonyms",
    "manage_security",
    "manage_service_account",
    "manage_slm",
    "manage_token",
    "manage_transform",
    "manage_user_profile",
    "manage_watcher",
    "monitor",
    "monitor_connector",
    "monitor_data_frame_transforms",
    "monitor_data_stream_global_retention",
    "monitor_enrich",
    "monitor_inference",
    "monitor_ml",
    "monitor_rollup",
    "monitor_snapshot",
    "monitor_stats",
    "monitor_text_structure",
    "monitor_transform",
    "monitor_watcher",
    "none",
    "post_behavioral_analytics_event",
    "read_ccr",
    "read_connector_secrets",
    "read_fleet_secrets",
    "read_ilm",
    "read_pipeline",
    "read_security",
    "read_slm",
    "transport_client",
    "write_connector_secrets",
    "write_fleet_secrets"
)

val INDEX_PRIVILEGES = setOf(
    "all",
    "auto_configure",
    "create",
    "create_doc",
    "create_index",
    "cross_cluster_replication",
    "cross_cluster_replication_internal",
    "delete",
    "delete_index",
    "index",
    "maintenance",
    "manage",
    "manage_data_stream_lifecycle",
    "manage_failure_store",
    "manage_follow_index",
    "manage_ilm",
    "manage_leader_index",
    "monitor",
    "none",
    "read",
    "read_cross_cluster",
    "read_failure_store",
    "view_index_metadata",
    "write"
)

const val INDEX_INDEX_PRIVILEGE =   "index"
const val WRITE_INDEX_PRIVILEGE =   "write"
const val MANAGE_INDEX_PRIVILEGE =  "manage"
const val CREATE_INDEX_PRIVILEGE =  "create_index"
const val DELETE_INDEX_PRIVILEGE =  "delete_index"

val REMOTE_CLUSTER_PRIVILEGES = setOf(
    "monitor_enrich",
    "monitor_stats"
)

val ENGINEER_INDEX_PRIVILEGES = listOf(
    "create",
    "create_doc",
    "create_index",
    "cross_cluster_replication",
    "cross_cluster_replication_internal",
    "delete",
    "index",
    "maintenance",
    "monitor",
    "read",
    "read_cross_cluster",
    "read_failure_store",
    "view_index_metadata",
    "write"
)

val DEVOPS_INDEX_PRIVILEGES = listOf("all")
val DEVOPS_CLUSTER_PRIVILEGES = listOf("all")
