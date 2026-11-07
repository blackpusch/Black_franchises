# 1. Proyecto en MongoDB Atlas
resource "mongodbatlas_project" "project" {
  name   = var.project_name
  org_id = var.mongodbatlas_org_id
}

# 2. Cluster Avanzado en AWS (M0 Free Tier o superior)
resource "mongodbatlas_advanced_cluster" "cluster" {
  project_id   = mongodbatlas_project.project.id
  name         = var.cluster_name
  cluster_type = "REPLICASET"

  replication_specs {
    region_configs {
      electable_specs {
        instance_size = var.provider_instance_size_name
      }
      provider_name         = "TENANT"
      backing_provider_name = "AWS"
      region_name           = var.atlas_region
      priority              = 7
    }
  }
}

# 3. Usuario de base de datos con permisos readWrite sobre 'franchises'
resource "mongodbatlas_database_user" "user" {
  username           = var.db_username
  password           = var.db_password
  project_id         = mongodbatlas_project.project.id
  auth_database_name = "admin"

  roles {
    role_name     = "readWrite"
    database_name = var.database_name
  }
}

# 4. Regla de acceso de red (Access List)
resource "mongodbatlas_project_ip_access_list" "ip_access" {
  project_id = mongodbatlas_project.project.id
  cidr_block = var.whitelist_cidr
  comment    = "Regla de acceso de red para la API de franquicias"
}
