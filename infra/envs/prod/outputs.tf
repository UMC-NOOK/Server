output "instance_id" {
  value = module.server.instance_id
}

output "private_ip" {
  value = module.server.private_ip
}

output "public_ip" {
  value = module.server.public_ip
}

output "security_group_id" {
  value = module.server.security_group_id
}

output "database_address" {
  value = module.database.address
}

output "database_master_user_secret_arn" {
  value = module.database.master_user_secret_arn
}
