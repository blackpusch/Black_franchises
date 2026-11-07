variable "mongodbatlas_public_key" {
  description = "Public Programmatic API key de MongoDB Atlas"
  type        = String
}

variable "mongodbatlas_private_key" {
  description = "Private Programmatic API key de MongoDB Atlas"
  type        = String
  sensitive   = true
}

variable "mongodbatlas_org_id" {
  description = "ID de la organizacion en MongoDB Atlas"
  type        = String
}

variable "project_name" {
  description = "Nombre del proyecto en MongoDB Atlas"
  type        = String
  default     = "franchises-project"
}

variable "cluster_name" {
  description = "Nombre del cluster de MongoDB"
  type        = String
  default     = "franchises-cluster"
}

variable "atlas_region" {
  description = "Region de AWS para alojar el cluster de MongoDB Atlas"
  type        = String
  default     = "US_EAST_1"
}

variable "provider_instance_size_name" {
  description = "Tier de la instancia de MongoDB (M0 para Free Tier, M10 para produccion)"
  type        = String
  default     = "M0"
}

variable "db_username" {
  description = "Nombre de usuario de base de datos"
  type        = String
  default     = "franchise_user"
}

variable "db_password" {
  description = "Contraseña para el usuario de base de datos"
  type        = String
  sensitive   = true
}

variable "database_name" {
  description = "Nombre de la base de datos de la aplicacion"
  type        = String
  default     = "franchises"
}

variable "whitelist_cidr" {
  description = "Bloque CIDR permitido en la lista de acceso de red (0.0.0.0/0 para contenedores cloud/serverless)"
  type        = String
  default     = "0.0.0.0/0"
}
