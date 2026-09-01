output "instance_id" {
  value = module.server.instance_id
}

output "private_ip" {
  value = module.server.private_ip
}

output "public_ip" {
  value = module.server.public_ip
}

output "domain_name" {
  value = var.domain_name
}

output "security_group_id" {
  value = module.server.security_group_id
}
