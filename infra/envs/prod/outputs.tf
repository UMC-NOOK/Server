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

output "vpc_id" {
  description = "Prod VPC ID — use as vpc_id in the monitoring env tfvars."
  value       = module.network.vpc_id
}

output "dns_name_servers" {
  description = "Route53 name servers. Paste these into the domain registrar when enable_route53 = true."
  value       = var.enable_route53 ? module.dns[0].name_servers : []
}
