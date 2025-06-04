variable "Team" {
  description = "Team tag"
  default     = "devcos5-team06"
}

variable "region" {
  description = "region"
  default     = "ap-northeast-1"
}

variable "nickname" {
  description = "nickname"
  default     = "moviematch"
}

variable "prefix" {
  description = "Team"
  default     = "team06"
}

variable "github_access_token_1" {
  description = "GitHub 컨테이너 레지스트리 인증 토큰"
  type        = string
  sensitive   = true
}

variable "github_access_token_1_owner" {
  description = "GitHub 토큰 소유자 ID"
  type        = string
}

variable "mysql_root_password" {
  description = "MySQL 루트 비밀번호"
  type        = string
  sensitive   = true
}

variable "mysql_user_password_1" {
  description = "MySQL 내부 사용자 비밀번호"
  type        = string
  sensitive   = true
}

variable "mysql_user_password_2" {
  description = "MySQL 외부 사용자 비밀번호"
  type        = string
  sensitive   = true
}

variable "bucket_name" {
  description = "MySQL 외부 사용자 비밀번호"
  type        = string
  sensitive   = true
}