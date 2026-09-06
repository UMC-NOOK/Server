output "zone_id" {
  value = local.zone_id
}

output "name_servers" {
  description = "Route53 name servers — paste these into the domain registrar."
  value       = var.create_zone ? aws_route53_zone.this[0].name_servers : []
}
