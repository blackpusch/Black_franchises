variable "mongodbatlas_public_key" {
  description = "Public Programmatic API key de MongoDB Atlas"
  type        = string

  validation {
    condition     = length(trimspace(var.mongodbatlas_public_key)) > 0 && var.mongodbatlas_public_key != "your-atlas-public-key"
    error_message = "mongodbatlas_public_key no puede estar vacio ni usar el valor de plantilla 'your-atlas-public-key'."
  }
}

variable "mongodbatlas_private_key" {
  description = "Private Programmatic API key de MongoDB Atlas"
  type        = string
  sensitive   = true

  validation {
    condition     = length(trimspace(var.mongodbatlas_private_key)) > 0 && var.mongodbatlas_private_key != "your-atlas-private-key-uuid"
    error_message = "mongodbatlas_private_key no puede estar vacio ni usar el valor de plantilla 'your-atlas-private-key-uuid'."
  }
}

variable "mongodbatlas_org_id" {
  description = "ID de la organizacion en MongoDB Atlas"
  type        = string

  validation {
    condition     = length(trimspace(var.mongodbatlas_org_id)) > 0 && var.mongodbatlas_org_id != "your-atlas-organization-id"
    error_message = "mongodbatlas_org_id no puede estar vacio ni usar el valor de plantilla 'your-atlas-organization-id'."
  }
}

variable "project_name" {
  description = "Nombre del proyecto en MongoDB Atlas"
  type        = string
  default     = "franchises-project"
}

variable "cluster_name" {
  description = "Nombre del cluster de MongoDB"
  type        = string
  default     = "franchises-cluster"
}

variable "atlas_region" {
  description = "Region de AWS para alojar el cluster de MongoDB Atlas"
  type        = string
  default     = "US_EAST_1"
}

variable "provider_instance_size_name" {
  description = "Tier de la instancia de MongoDB (M0 para Free Tier, M10 para produccion)"
  type        = string
  default     = "M0"
}

variable "db_username" {
  description = "Nombre de usuario de base de datos"
  type        = string
  default     = "franchise_user"
}

variable "db_password" {
  description = "Contraseña para el usuario de base de datos (en texto plano y sin pre-codificar; outputs.tf aplica URL-encoding automaticamente en la URI)"
  type        = string
  sensitive   = true

  validation {
    condition     = length(trimspace(var.db_password)) > 0 && var.db_password != "ChangeMeStrongPassword2026!"
    error_message = "db_password no puede estar vacio ni usar el valor de plantilla 'ChangeMeStrongPassword2026!'."
  }
}

variable "database_name" {
  description = "Nombre de la base de datos de la aplicacion"
  type        = string
  default     = "franchises"
}

variable "whitelist_cidr" {
  description = "Bloque CIDR permitido en la lista de acceso de red (0.0.0.0/0 para contenedores cloud/serverless)"
  type        = string
  default     = "0.0.0.0/0"
}
