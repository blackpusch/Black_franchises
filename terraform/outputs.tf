output "project_id" {
  description = "ID del proyecto creado en MongoDB Atlas"
  value       = mongodbatlas_project.project.id
}

output "cluster_name" {
  description = "Nombre del cluster aprovisionado"
  value       = mongodbatlas_advanced_cluster.cluster.name
}

output "standard_srv_connection_string" {
  description = "Connection string SRV estandar proporcionado por Atlas"
  value       = mongodbatlas_advanced_cluster.cluster.connection_strings[0].standard_srv
}

output "mongodb_connection_uri" {
  description = "URI completa de conexion reactiva lista para inyectar en MONGODB_URI de Spring Boot"
  sensitive   = true
  value       = "mongodb+srv://${var.db_username}:${var.db_password}@${replace(mongodbatlas_advanced_cluster.cluster.connection_strings[0].standard_srv, "mongodb+srv://", "")}/${var.database_name}?retryWrites=true&w=majority"
}
