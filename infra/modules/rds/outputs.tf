output "endpoint" {
  value = aws_db_instance.this.endpoint
}

output "address" {
  value = aws_db_instance.this.address
}

output "security_group_id" {
  value = aws_security_group.this.id
}

output "master_user_secret_arn" {
  description = "Secrets Manager ARN containing the RDS master credentials."
  value       = try(aws_db_instance.this.master_user_secret[0].secret_arn, null)
}
