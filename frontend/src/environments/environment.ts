export const environment = {
  production: false,
  // Durante desarrollo proxyará a Tomcat para evitar CORS
  // URL absoluta apuntando al contexto correcto en Tomcat (sin barra final para normalizar)
  apiBaseUrl: 'http://localhost:8080/Horizontes'
};
