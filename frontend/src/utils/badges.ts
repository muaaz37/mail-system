export const getSeverityBadge = (status: string) => {
  switch (status) {
    case 'SENT':
      return 'success';
    case 'DRAFT':
      return 'info';
    case 'ERROR':
      return 'danger';
    default:
      return 'info';
  }
}


export const getSourceBadge = (source: string) => {
  switch (source) {
    case 'INTERN':
      return 'secondary';
    case 'EXTERN':
      return 'warn';
    default:
      return 'info';
  }
}
